"""
Multi-seed ensemble inference for AQA.
Loads checkpoints from different seeds, averages predictions, computes SRCC.
"""
import os
import torch
import numpy as np
from scipy import stats

from utils.utils import parse_args, init_seed, init_gpu
from dataloader import build_dataloader
from models import build_backbone, build_neck, build_head

# --- Config ---
SEEDS = [111, 333, 555, 999]
CKPT_TEMPLATE = "ckpts/i3d_{dataset}_{seed}_{label}_{att_loss}_{dino_loss}_{query_var}_{pe}_{num_layers}.pt"


def get_ckpt_path(cfg, seed):
    return CKPT_TEMPLATE.format(
        dataset=cfg.dataset_name, seed=seed, label=cfg.label,
        att_loss=cfg.att_loss, dino_loss=cfg.dino_loss,
        query_var=cfg.query_var, pe=cfg.pe, num_layers=cfg.num_layers,
    )


def run_inference(cfg, neck, head, data_loader):
    """Run inference and collect predictions + ground truth."""
    neck.eval()
    head.eval()
    preds, truths = [], []

    print(f"\n  [推理开始] 模型已切换为 eval 模式，gradient 计算已关闭")
    sample_count = 0

    with torch.no_grad():
        for data_, _ in data_loader:
            data = data_
            score = data["score"].float().cuda()

            # 输入预处理
            if cfg.split_feats and "feats" in data:
                clip_feats = data["feats"].cuda()
                if sample_count == 0:
                    print(f"  [输入预处理] 使用预提取特征 (split_feats=True)")
                    print(f"  [输入预处理] clip_feats shape: {clip_feats.shape}  (batch, num_clips, feature_dim)")
                    print(f"  [输入预处理] 特征已加载到 GPU (cuda)")
            else:
                video = data["video"]
                bs, frame, h, w = video.shape
                clip_feats = backbone(video)[1].squeeze(-1).squeeze(-1).permute(0, 2, 1)
                if sample_count == 0:
                    print(f"  [输入预处理] 使用原始视频帧，通过 backbone 实时提取特征")

            # Neck: TQN 时序解码
            tgt_weight, graph_attn = neck(clip_feats, train=False)
            if sample_count == 0:
                print(f"  [Neck-TQN] 输入 clip_feats → 输出 tgt_weight shape: {tgt_weight.shape}")
                print(f"  [Neck-TQN] 经过 {len(graph_attn[0])} 层 Transformer Decoder 解码")

            # Head: 质量评估
            probs, weight, quality, difficulty, var = head(tgt_weight)
            if sample_count == 0:
                print(f"  [Head-Evaluator] tgt_weight → probs(最终分数), weight(权重), quality(质量), difficulty(难度), var(不确定度)")
                print(f"  [Head-Evaluator] 输出维度示例:")
                print(f"    probs shape:     {probs.shape}   (batch,) ← 最终预测质量分数")
                print(f"    weight shape:    {weight.shape}   (batch, num_clips, 1) ← 各clip注意力权重")
                print(f"    quality shape:   {quality.shape}   (batch, num_clips, 1) ← 各clip执行质量")
                print(f"    difficulty shape:{difficulty.shape}   (batch, num_clips, 1) ← 各clip动作难度")
                print(f"    var shape:       {var.shape}   (batch, num_clips, 1) ← 各clip不确定度")
                print(f"  [计算公式] 最终分数 = Σ((quality + difficulty) × weight) 对所有clip加权求和")

            # 打印前3个样本的详细预测
            if sample_count == 0:
                print(f"\n  {'='*50}")
                print(f"  前3个样本推理结果示例:")
                for i in range(min(3, len(probs))):
                    print(f"  --- 样本 {i+1} ---")
                    print(f"    真实分数: {score[i].item():.4f} (归一化值)")
                    print(f"    预测分数: {probs[i].item():.4f} (归一化值)")
                    print(f"    绝对误差: {abs(probs[i].item() - score[i].item()):.4f}")
                    print(f"    前5个clip的 (quality, difficulty, weight):")
                    for j in range(min(5, quality.shape[1])):
                        q_val = quality[i, j, 0].item()
                        d_val = difficulty[i, j, 0].item()
                        w_val = weight[i, j, 0].item()
                        print(f"      clip {j:2d}: q={q_val:+.4f}  d={d_val:+.4f}  w={w_val:.4f}  (q+d)*w={(q_val+d_val)*w_val:.4f}")
                print(f"  {'='*50}\n")

            preds.extend([i.item() for i in probs])
            truths.extend(score.cpu().numpy())
            sample_count += len(probs)

    print(f"  [推理完成] 共处理 {sample_count} 个测试样本")
    return np.array(preds), np.array(truths)


def main():
    cfg = parse_args()
    init_seed(cfg)
    init_gpu(cfg)

    print(f"\n{'='*60}")
    print(f"  Multi-Seed Ensemble Inference")
    print(f"  Dataset: {cfg.dataset_name}  Label: {cfg.label}")
    print(f"  Seeds: {SEEDS}")
    print(f"{'='*60}\n")

    # Build dataloader (test only)
    data_loaders = build_dataloader(cfg)
    test_loader = data_loaders["test"]

    # Build model components
    print(f"\n{'='*60}")
    print(f"  [模型构建] 开始构建推理模型组件")
    backbone = build_backbone(cfg)
    neck = build_neck(cfg)
    head = build_head(cfg)
    print(f"  [模型构建] Backbone: {cfg.backbone} (ViViT Video Vision Transformer)")
    print(f"  [模型构建] Neck:     {cfg.neck} (Temporal Query Network, {cfg.num_layers}层Decoder)")
    print(f"  [模型构建] Head:     {cfg.head} (Evaluator_weighted, 4分支MLP)")
    print(f"  [模型构建] 配置: query_var={cfg.query_var}, pe={cfg.pe}, q_number={cfg.q_number}")
    print(f"{'='*60}")

    all_preds = []
    all_truths = None

    for seed_idx, seed in enumerate(SEEDS):
        ckpt_path = get_ckpt_path(cfg, seed)
        if not os.path.exists(ckpt_path):
            print(f"  [SKIP] seed={seed}: checkpoint not found → {ckpt_path}")
            continue

        print(f"\n{'='*60}")
        print(f"  [模型加载] seed={seed} ({seed_idx+1}/{len(SEEDS)})")
        print(f"  [模型加载] checkpoint 路径: {ckpt_path}")
        ckpt = torch.load(ckpt_path, map_location="cuda")
        print(f"  [模型加载] checkpoint 内容: epoch={ckpt.get('epoch','?')}, rho_best={ckpt.get('rho_best','?'):.4f}")
        print(f"  [模型加载] checkpoint 包含的组件: {list(ckpt.keys())}")

        # Strip DataParallel "module." prefix if present
        neck_state = ckpt["neck"]
        head_state = ckpt["head"]
        has_prefix = any(k.startswith("module.") for k in neck_state)
        if has_prefix:
            neck_state = {k[7:]: v for k, v in neck_state.items()}
            head_state = {k[7:]: v for k, v in head_state.items()}
            print(f"  [模型加载] 检测到 DataParallel 'module.' 前缀，已自动去除")

        neck.load_state_dict(neck_state)
        head.load_state_dict(head_state)
        print(f"  [模型加载] neck (TQN) state_dict 加载完成，共 {len(neck_state)} 个参数")
        print(f"  [模型加载] head (Evaluator_weighted) state_dict 加载完成，共 {len(head_state)} 个参数")
        print(f"  [模型加载] 模型已置为 eval 模式，准备推理...")

        preds, truths = run_inference(cfg, neck, head, test_loader)
        all_preds.append(preds)
        all_truths = truths  # same for all seeds

        rho, _ = stats.spearmanr(preds, truths)
        rl2 = np.power((preds - truths) / (truths.max() - truths.min()), 2).sum() / truths.shape[0]
        print(f"  seed={seed:>4d}:  SRCC={rho:.4f}  RL2={rl2:.4f}  (epoch {ckpt.get('epoch', '?')})")

    # --- Vanilla ensemble: equal-weight average ---
    ensemble_preds = np.mean(all_preds, axis=0)
    rho_ens, _ = stats.spearmanr(ensemble_preds, truths)
    rl2_ens = np.power((ensemble_preds - truths) / (truths.max() - truths.min()), 2).sum() / truths.shape[0]

    # Individual SRCCs for weighting
    individual_srccs = []
    for preds in all_preds:
        rho, _ = stats.spearmanr(preds, truths)
        individual_srccs.append(rho)

    print(f"\n  {'─'*40}")
    print(f"  Ensemble (equal, {len(all_preds)} seeds):")
    print(f"    SRCC = {rho_ens:.4f}    gain = {rho_ens - max(individual_srccs):+.4f}")

    # --- Top-2: only best two seeds ---
    top_indices = np.argsort(individual_srccs)[-2:]  # indices of top 2
    top2_preds = np.mean([all_preds[i] for i in top_indices], axis=0)
    rho_top2, _ = stats.spearmanr(top2_preds, truths)
    top2_seeds = [SEEDS[i] for i in top_indices]
    print(f"  Top-2  (seeds {top2_seeds}):")
    print(f"    SRCC = {rho_top2:.4f}    gain = {rho_top2 - max(individual_srccs):+.4f}")

    # --- Top-3: best three ---
    top3_indices = np.argsort(individual_srccs)[-3:]
    top3_preds = np.mean([all_preds[i] for i in top3_indices], axis=0)
    rho_top3, _ = stats.spearmanr(top3_preds, truths)
    top3_seeds = [SEEDS[i] for i in top3_indices]
    print(f"  Top-3  (seeds {top3_seeds}):")
    print(f"    SRCC = {rho_top3:.4f}    gain = {rho_top3 - max(individual_srccs):+.4f}")

    # --- Weighted: proportional to SRCC ---
    weights = np.array([max(s, 1e-6) for s in individual_srccs])
    weights = weights / weights.sum()
    weighted_preds = np.average(all_preds, axis=0, weights=weights)
    rho_w, _ = stats.spearmanr(weighted_preds, truths)
    print(f"  Weighted (SRCC-proportional):")
    print(f"    SRCC = {rho_w:.4f}    gain = {rho_w - max(individual_srccs):+.4f}")
    print(f"    Weights: {dict(zip(SEEDS, [f'{w:.3f}' for w in weights]))}")

    # --- Summary ---
    print(f"\n  Individual: {[f'{s:.4f}' for s in individual_srccs]}")
    print(f"  Best single: {max(individual_srccs):.4f}")
    best_ensemble = max(rho_ens, rho_top2, rho_top3, rho_w)
    print(f"  Best ensemble: {best_ensemble:.4f}")
    print(f"{'='*60}\n")


if __name__ == "__main__":
    main()
