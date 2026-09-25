import torch.nn as nn
import torch


class Evaluator_weighted(nn.Module):
    def __init__(self):
        super().__init__()

        # Quality branch — how well each clip is executed
        self.quality_fc1 = nn.Linear(1024, 512)
        self.quality_fc2 = nn.Linear(512, 256)
        self.quality_fc3 = nn.Linear(256, 1)

        # Difficulty branch — how hard each clip element is
        self.diff_fc1 = nn.Linear(1024, 512)
        self.diff_fc2 = nn.Linear(512, 256)
        self.diff_fc3 = nn.Linear(256, 1)

        # Weight branch — attention weight for each clip
        self.weight_fc1 = nn.Linear(1024, 512)
        self.weight_fc2 = nn.Linear(512, 256)
        self.weight_fc3 = nn.Linear(256, 1)

        # Variance branch — uncertainty per clip
        self.var_fc1 = nn.Linear(1024, 512)
        self.var_fc2 = nn.Linear(512, 256)
        self.var_fc3 = nn.Linear(256, 1)

        self.dropout = nn.Dropout(p=0.5)
        self.softmax = nn.Softmax(dim=-2)

    def forward(self, x):
        # Quality per clip
        q = torch.relu(self.quality_fc1(x))
        q = torch.relu(self.quality_fc2(q))
        q = self.quality_fc3(q)

        # Difficulty per clip
        d = torch.relu(self.diff_fc1(x))
        d = torch.relu(self.diff_fc2(d))
        d = self.diff_fc3(d)

        # Clip score = quality + difficulty (additive decomposition)
        clip_score = q + d

        # Weight per clip
        w = torch.relu(self.weight_fc1(x))
        w = torch.relu(self.weight_fc2(w))
        w = self.softmax(self.weight_fc3(w))

        # Variance (uncertainty) per clip
        v = torch.relu(self.var_fc1(x))
        v = torch.relu(self.var_fc2(v))
        v = torch.abs(self.var_fc3(v))

        # Final score = weighted sum of clip scores
        logits = torch.sum(clip_score.squeeze(-1) * w.squeeze(-1), axis=1)

        return logits, w, q, d, v
