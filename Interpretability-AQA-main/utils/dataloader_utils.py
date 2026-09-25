import numpy as np
from utils.torchvideotransforms import video_transforms, volume_transforms

def worker_init_fn(worker_id):
    np.random.seed(np.random.get_state()[1][0] + worker_id)


class _SeedWorker:
    """Picklable worker_init_fn that seeds numpy deterministically from cfg.seed."""
    def __init__(self, base_seed):
        self.base_seed = base_seed

    def __call__(self, worker_id):
        np.random.seed(self.base_seed + worker_id)


def seed_worker(base_seed):
    return _SeedWorker(base_seed)


def get_video_trans():
    train_trans = video_transforms.Compose([
        video_transforms.RandomHorizontalFlip(),
        video_transforms.Resize((224, 224)),
        video_transforms.RandomCrop(224),
        volume_transforms.ClipToTensor(),
        video_transforms.Normalize(mean=[0.485, 0.456, 0.406], std=[0.229, 0.224, 0.225]),
    ])
    test_trans = video_transforms.Compose([
        video_transforms.Resize((224, 224)),
        video_transforms.CenterCrop(224),
        volume_transforms.ClipToTensor(),
        video_transforms.Normalize(mean=[0.485, 0.456, 0.406], std=[0.229, 0.224, 0.225]),
    ])
    return train_trans, test_trans
