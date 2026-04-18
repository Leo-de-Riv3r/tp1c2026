import random
from dataclasses import dataclass
from itertools import count
import numpy as np

MAX_VEC_LENGHT = 25
ALLOWED_COORD_VALUES = [-1, 0, 1]
ALLOWED_COORD_PROBABILITIES = [0.6, 0.2, 0.2]
NEW_SAMPLES_PER_ITERATION = 2
UPDATE_PROBABILITY = 0.1
ITERATIONS = 2000
K = 10
INITIAL_SAMPLE_SIZE = 200
INITIAL_ANCHOR_SIZE = 5

type vec_values = np.ndarray[tuple[int], np.dtype[np.int_]]

@dataclass
class Vec:
    id: int
    values: vec_values

_vec_id_counter = count(1)

def sample_vec():
    values = np.random.choice(
        ALLOWED_COORD_VALUES, size=MAX_VEC_LENGHT, p=ALLOWED_COORD_PROBABILITIES
    )
    return Vec(id=next(_vec_id_counter), values=values)

def similarity(vec1: Vec, vec2: Vec) -> int:
    v1, v2 = vec1.values, vec2.values
    return int(np.sum((v1 == v2) & (v1 != 0)))

def complement(vec1: Vec, vec2: Vec) -> int:
    v1, v2 = vec1.values, vec2.values
    return int(np.sum((v1 == -v2) & (v1 != 0)))

def closest_neighbor(vec: Vec, anchors: list[Vec]) -> Vec:
    return max(anchors, key=lambda a: similarity(vec, a))

def average_k_neighbors(vecs: list[Vec]) -> vec_values | None:
    if not vecs:
        return None
    matrix = np.stack([v.values for v in vecs])   # shape (n, MAX_VEC_LENGHT)
    return np.round(matrix.mean(axis=0)).astype(int)


samples: list[Vec] = [sample_vec() for _ in range(INITIAL_SAMPLE_SIZE)]
anchors: list[Vec] = random.sample(samples, INITIAL_ANCHOR_SIZE)
neighbours: dict[int, list[Vec]] = {anchor.id: [] for anchor in anchors}

def assign_new_samples():
    for _ in range(NEW_SAMPLES_PER_ITERATION):
        sample = sample_vec()
        neighbours[closest_neighbor(sample, anchors).id].append(sample)
             
def update_samples():
    for sample in samples:
        for anchor in anchors:
            if sample in neighbours[anchor.id]:
                neighbours[anchor.id].remove(sample)

        if random.random() < UPDATE_PROBABILITY:
            sample.values = np.random.choice(
                ALLOWED_COORD_VALUES, size=MAX_VEC_LENGHT, p=ALLOWED_COORD_PROBABILITIES
            )

        neighbours[closest_neighbor(sample, anchors).id].append(sample)
                     
def update_anchors():
    new_neighbours: list[tuple[Vec, list[Vec]]] = []
    for anchor_id in neighbours.keys():
        anchor = next(a for a in anchors if a.id == anchor_id)
        # get the k closest neighbors to the anchor
        to_sample = min(len(neighbours[anchor_id]), K)
        k_nearest = sorted(neighbours[anchor_id], key=lambda s: similarity(s, anchor), reverse=True)[:to_sample]
        z : vec_values | None = average_k_neighbors(k_nearest)
        anchor.values = z if z is not None else anchor.values
    return new_neighbours

def average_distance_between_anchors() -> float:
    matrix = np.stack([a.values for a in anchors], dtype=float)
    diff = matrix[:, np.newaxis, :] - matrix[np.newaxis, :, :]   # (K, K, D)
    dist = np.linalg.norm(diff, axis=-1)                          # (K, K)
    n = len(anchors)
    total = dist[np.triu_indices(n, k=1)].sum()
    return float(total / (n * (n - 1) / 2))

         
distances : list[float] = []
        
for i in range(ITERATIONS):
    if i % 5 == 0:
        print(f"Iteration {i}")
    assign_new_samples()
    update_samples()
    update_anchors()
    distances.append(average_distance_between_anchors())

## plot the average distance between anchors
import matplotlib.pyplot as plt
plt.plot(distances)
plt.xlabel("Iterations")
plt.ylabel("Average distance between anchors")
plt.title("Average distance between anchors and average anchor population")
plt.show()