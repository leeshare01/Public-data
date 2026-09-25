
import os
import csv
import torch
from tqdm import tqdm
import logging
from datetime import datetime
from utils.utils import log_and_print
from utils.vis import *
from loss import attention_loss, cal_spearmanr_rl2
#from methods.weight_methods import NashMTL

import torch
import torch.nn as nn

class NashMTL(nn.Module):
    def __init__(self, num_tasks, device):
        super().__init__()
        self.num_tasks = num_tasks
        self.device = device
        self.alpha = nn.Parameter(torch.ones(num_tasks))

    def forward(self, losses):
        return sum(losses)


import matplotlib.pyplot as plt
def run(cfg, base_logger, network, data_loaders, kld, mse, optimizer, scheduler, splits=["train", "test"]):
    backbone, neck, head = network

    patience = getattr(cfg, 'early_stop_patience', 0)
    no_improve = 0
    rho_best, epoch_best, rl2_best = 0, 0, 0

    test_only = "train" not in splits
    if test_only:
        cfg.epoch_num = 400

    # --- Training header ---
    header = (
        f"\n{'='*75}\n"
        f"  Dataset: {cfg.dataset_name}  |  Label: {cfg.label}  |  Seed: {cfg.seed}\n"
        f"  Backbone: {cfg.backbone}  |  Neck: {cfg.neck}  |  Layers: {cfg.num_layers}  |  Query Var: {cfg.query_var}  |  PE: {cfg.pe}\n"
        f"  Att Loss: {cfg.att_loss}  |  Dino Loss: {cfg.dino_loss}  |  Epochs: {cfg.epoch_num}  |  LR: {cfg.lr}  |  Batch: {cfg.bs_train}\n"
        f"{'='*75}"
    )
    log_and_print(base_logger, header)
    log_and_print(base_logger, f"{'Epoch':>5s}  | {'Train SRCC':>11s}  | {'Test SRCC':>10s}  | {'Best SRCC':>10s}  | {'RL2':>10s}  | Note")
    log_and_print(base_logger, f"{'-'*5}--+{'-'*12}-+{'-'*11}-+{'-'*11}-+{'-'*11}-+{'-'*20}")

    # CSV output for plotting
    timestamp = datetime.now().strftime("%m%d_%H%M")
    csv_path = f'exp/{timestamp}_{cfg.seed}_{cfg.dataset_name}_{cfg.label}_{cfg.att_loss}_{cfg.dino_loss}_{cfg.query_var}_{cfg.pe}.csv'
    os.makedirs('exp', exist_ok=True)
    csv_file = open(csv_path, 'w', newline='')
    csv_writer = csv.writer(csv_file)
    csv_writer.writerow(['epoch', 'train_srcc', 'test_srcc', 'best_srcc', 'rl2'])

    for epoch in range(cfg.epoch_num):
        epoch_rho = {}

        for split in splits:
            true_scores = []
            pred_scores = []

            if split == 'train':
                backbone.train()
                head.train()
                neck.train()
                torch.set_grad_enabled(True)
            else:
                backbone.eval()
                head.eval()
                neck.eval()
                torch.set_grad_enabled(False)

            self_map_lst = []
            cross_map_lst = []
            losses = 0
            for data_ in data_loaders[split]:
                data, clip_info = data_
                score = data["score"].float().cuda()
                video = data["video"]
                if cfg.split_feats:
                    if "feats" in data:
                        clip_feats = data["feats"].cuda()
                    else:
                        bs, frame, feats = video.shape
                        video = video.reshape(video.shape[0], 3, 48, 16, 224, 224).cuda()
                        clip_feats = torch.empty(bs, video.shape[2], feats).cuda()
                        for i in range(frame):
                            clip_feats[:, i] = backbone(video[:, :, i, :, :, :].cuda())[1].squeeze(-1).squeeze(-1).squeeze(-1)
                else:
                    bs, frame, h, w = video.shape
                    clip_feats = backbone(video)[1].squeeze(-1).squeeze(-1).permute(0, 2, 1)

                tgt_weight, graph_attn = neck(clip_feats, train=False)
                probs, weight, quality, difficulty, var = head(tgt_weight)

                pred_scores.extend([i.item() for i in probs])
                true_scores.extend(score.cpu().numpy())

                kld_loss, self_map_lst, cross_map_lst = attention_loss(graph_attn, kld, self_map_lst, cross_map_lst)

                if cfg.dino_loss and split == "train":
                    probs = probs + torch.randn_like(probs).normal_(mean=0.0, std=0.05)
                mes_loss = mse(probs, score)
                if cfg.att_loss:
                    loss = mes_loss + kld_loss
                else:
                    loss = mes_loss

                losses += loss
                rho, p, rl2 = cal_spearmanr_rl2(pred_scores, true_scores)

                if split == "train":
                    optimizer.zero_grad()
                    loss.backward()
                    optimizer.step()
                    scheduler.step()

            epoch_rho[split] = (rho, rl2)

        # --- Print single-line epoch summary ---
        train_rho, _ = epoch_rho.get("train", (float('nan'), float('nan')))
        test_rho, test_rl2 = epoch_rho.get("test", (float('nan'), float('nan')))

        is_new_best = test_rho > rho_best
        if is_new_best:
            rho_best = test_rho
            epoch_best = epoch
            rl2_best = test_rl2
            no_improve = 0
            note = "★ NEW BEST"
            if not test_only:
                torch.save({
                    'epoch': epoch,
                    'backbone': backbone.state_dict(),
                    'neck': neck.state_dict(),
                    'head': head.state_dict(),
                    'optimizer': optimizer.state_dict(),
                    'rho_best': rho_best
                }, f'ckpts/i3d_{cfg.dataset_name}_{cfg.seed}_{cfg.label}_{cfg.att_loss}_{cfg.dino_loss}_{cfg.query_var}_{cfg.pe}_{cfg.num_layers}.pt')
        else:
            no_improve += 1
            note = ""

        line = f"{epoch:>5d}  | {train_rho:>11.4f}  | {test_rho:>10.4f}  | {rho_best:>10.4f}  | {test_rl2:>10.4f}  | {note}"
        log_and_print(base_logger, line)
        csv_writer.writerow([epoch, f"{train_rho:.4f}", f"{test_rho:.4f}", f"{rho_best:.4f}", f"{test_rl2:.4f}"])

        if patience > 0 and no_improve >= patience:
            log_and_print(base_logger, f"\nEarly stopping: no improvement for {patience} epochs.")
            break

    # --- Final summary ---
    csv_file.close()
    stop_reason = "early stopped" if (patience > 0 and no_improve >= patience) else "completed"

    summary = (
        f"\n{'='*75}\n"
        f"  Training {stop_reason}!\n"
        f"  Dataset: {cfg.dataset_name}  Label: {cfg.label}  Seed: {cfg.seed}\n"
        f"  Best SRCC: {rho_best:.4f}  |  Best RL2: {rl2_best:.4f}  |  Best Epoch: {epoch_best}\n"
        f"  Config: query_var={cfg.query_var}  pe={cfg.pe}  att_loss={cfg.att_loss}  dino_loss={cfg.dino_loss}  layers={cfg.num_layers}\n"
        f"{'='*75}\n"
    )
    log_and_print(base_logger, summary)

    # --- Write to timestamped experiment record ---
    exp_name = f"{timestamp}_{cfg.dataset_name}_{cfg.label}"
    exp_path = f"exp/{exp_name}.log"
    os.makedirs("exp", exist_ok=True)
    with open(exp_path, "w") as f:
        f.write(header + "\n")
        f.write(f"  Started: {timestamp}\n")
        f.write(f"  Best SRCC: {rho_best:.4f}  |  Best RL2: {rl2_best:.4f}  |  Best Epoch: {epoch_best}\n")
        f.write(f"  Stop reason: {stop_reason}\n")
        f.write(f"{'='*75}\n")
    log_and_print(base_logger, f"Experiment record → {exp_path}")
            