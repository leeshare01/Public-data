import pandas as pd
import matplotlib.pyplot as plt
import numpy as np

# --- Load data ---
fisv = pd.read_csv("exp/999_fisv_TES_True_True_2_query_pe.csv")
rg   = pd.read_csv("exp/5_gym_Ribbon_True_True_2_query_pe.csv")

# --- Figure 1: Test SRCC comparison ---
fig, axes = plt.subplots(1, 3, figsize=(18, 5))

# Panel 1: Test SRCC
axes[0].plot(fisv["epoch"], fisv["test_srcc"], color="#2196F3", linewidth=1.2, label="Fis-V TES")
axes[0].plot(rg["epoch"],   rg["test_srcc"],   color="#E91E63", linewidth=1.2, label="RG Ribbon")
axes[0].axhline(y=0.717, color="#2196F3", linestyle="--", alpha=0.5, label="BMVC Fis-V (0.717)")
axes[0].axhline(y=0.858, color="#E91E63", linestyle="--", alpha=0.5, label="IJCV RG avg (0.858)")
axes[0].set_xlabel("Epoch")
axes[0].set_ylabel("Test SRCC")
axes[0].set_title("Test SRCC")
axes[0].legend(fontsize=8)
axes[0].grid(True, alpha=0.3)

# Panel 2: Train vs Test SRCC (Fis-V)
axes[1].plot(fisv["epoch"], fisv["train_srcc"], color="#90CAF9", linewidth=0.8, label="Train")
axes[1].plot(fisv["epoch"], fisv["test_srcc"],  color="#2196F3", linewidth=1.2, label="Test")
axes[1].axhline(y=fisv["best_srcc"].max(), color="green", linestyle="--", alpha=0.6, label=f"Best={fisv['best_srcc'].max():.4f}")
axes[1].set_xlabel("Epoch")
axes[1].set_ylabel("SRCC")
axes[1].set_title("Fis-V TES: Train vs Test")
axes[1].legend(fontsize=8)
axes[1].grid(True, alpha=0.3)

# Panel 3: Train vs Test SRCC (RG)
axes[2].plot(rg["epoch"], rg["train_srcc"], color="#F48FB1", linewidth=0.8, label="Train")
axes[2].plot(rg["epoch"], rg["test_srcc"],  color="#E91E63", linewidth=1.2, label="Test")
axes[2].axhline(y=rg["best_srcc"].max(), color="green", linestyle="--", alpha=0.6, label=f"Best={rg['best_srcc'].max():.4f}")
axes[2].set_xlabel("Epoch")
axes[2].set_ylabel("SRCC")
axes[2].set_title("RG Ribbon: Train vs Test")
axes[2].legend(fontsize=8)
axes[2].grid(True, alpha=0.3)

plt.tight_layout()
plt.savefig("exp/srcc_comparison.png", dpi=150)
print("Saved → exp/srcc_comparison.png")

# --- Figure 2: RL2 comparison ---
fig2, axes2 = plt.subplots(1, 2, figsize=(12, 4.5))

axes2[0].plot(fisv["epoch"], fisv["rl2"], color="#2196F3", linewidth=0.8)
axes2[0].set_xlabel("Epoch")
axes2[0].set_ylabel("R-ℓ₂")
axes2[0].set_title("Fis-V TES: R-ℓ₂")
axes2[0].grid(True, alpha=0.3)

axes2[1].plot(rg["epoch"], rg["rl2"], color="#E91E63", linewidth=0.8)
axes2[1].set_xlabel("Epoch")
axes2[1].set_ylabel("R-ℓ₂")
axes2[1].set_title("RG Ribbon: R-ℓ₂")
axes2[1].grid(True, alpha=0.3)

plt.tight_layout()
plt.savefig("exp/rl2_comparison.png", dpi=150)
print("Saved → exp/rl2_comparison.png")

# --- Print summary ---
print(f"\nFis-V TES: best SRCC={fisv['best_srcc'].max():.4f} at epoch {fisv['best_srcc'].idxmax()}")
print(f"RG Ribbon: best SRCC={rg['best_srcc'].max():.4f} at epoch {rg['best_srcc'].idxmax()}")
