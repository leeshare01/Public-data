"""Plot 4-seed PCS training curves — test SRCC over epochs."""
import pandas as pd
import matplotlib.pyplot as plt
import numpy as np

seeds = {
    111: ("exp/0522_2153_111_fisv_PCS_True_True_2_query_pe.csv", "#E91E63"),
    333: ("exp/0522_2323_333_fisv_PCS_True_True_2_query_pe.csv", "#2196F3"),
    555: ("exp/0522_2354_555_fisv_PCS_True_True_2_query_pe.csv", "#4CAF50"),
    999: ("exp/0522_0100_999_fisv_PCS_True_True_2_query_pe.csv", "#FF9800"),
}

fig, axes = plt.subplots(1, 2, figsize=(16, 5.5))

# Panel 1: Test SRCC
for seed, (path, color) in seeds.items():
    df = pd.read_csv(path)
    best_idx = df["best_srcc"].idxmax()
    axes[0].plot(df["epoch"], df["test_srcc"], color=color, linewidth=1.0, alpha=0.85)
    axes[0].scatter(best_idx, df.loc[best_idx, "test_srcc"], color=color, s=50, zorder=5,
                    edgecolors="white", linewidth=0.5)
    axes[0].annotate(f"s{seed}\n{df['best_srcc'].max():.4f}",
                     (best_idx, df.loc[best_idx, "test_srcc"]),
                     textcoords="offset points", xytext=(0, -22), ha="center",
                     fontsize=8, color=color, fontweight="bold")

axes[0].set_xlabel("Epoch")
axes[0].set_ylabel("Test SRCC")
axes[0].set_title("Fis-V PCS — 4-Seed Test SRCC")
axes[0].grid(True, alpha=0.3)
axes[0].set_xlim(0, max(df["epoch"].max() for _, (path, _) in seeds.items()))

# Panel 2: Train vs Test for each seed (small multiples)
colors = ["#E91E63", "#2196F3", "#4CAF50", "#FF9800"]
for idx, (seed, (path, _)) in enumerate(seeds.items()):
    df = pd.read_csv(path)
    ax = axes[1]
    ax.plot(df["epoch"], df["train_srcc"], color=colors[idx], linewidth=0.6,
            linestyle="--", alpha=0.5)
    ax.plot(df["epoch"], df["test_srcc"], color=colors[idx], linewidth=1.3,
            label=f"seed {seed} (best={df['best_srcc'].max():.4f})")
    best_idx = df["best_srcc"].idxmax()
    ax.axvline(x=best_idx, color=colors[idx], linewidth=0.5, alpha=0.3)

axes[1].set_xlabel("Epoch")
axes[1].set_ylabel("SRCC")
axes[1].set_title("Train (dashed) vs Test (solid)")
axes[1].legend(fontsize=7.5, loc="lower right")
axes[1].grid(True, alpha=0.3)

plt.tight_layout()
plt.savefig("exp/4seed_comparison.png", dpi=150)
print("Saved → exp/4seed_comparison.png")

# Summary table
print("\nSeed  Best SRCC  Best Epoch  RL2@best")
print("-" * 42)
for seed, (path, _) in seeds.items():
    df = pd.read_csv(path)
    best_idx = df["best_srcc"].idxmax()
    print(f"{seed:>4d}  {df['best_srcc'].max():>8.4f}  {best_idx:>10d}  {df.loc[best_idx, 'rl2']:>8.4f}")
