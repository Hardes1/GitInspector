import matplotlib.pyplot as plt
import numpy as np

class CompressionStatistics:
    def __init__(self, before : int, after : int, repo_url: str):
        self.data = []
        self.before = before
        self.after = after
        self.repo_url = repo_url

    def __str__(self) -> str:
        ratio = self.after / self.before if self.before != 0 else float('inf')
        return f"CompressionStatistics(before={self.before}, after={self.after}, ratio={ratio:.2f}, repo_url={self.repo_url})"

    def __repr__(self) -> str:
        return self.__str__()


def read_data(file_path) -> list[CompressionStatistics]:
    compression_statistics_data = []
    with open(file_path, "r") as f:
        for line in f:
            parts = line.strip().split(maxsplit=2)
            assert len(parts) == 3
            count1, count2, url = parts
            compression_statistics_data.append(
                CompressionStatistics(int(count1), int(count2), url)
            )
    return compression_statistics_data


def main():
    semantic_diff_list = read_data("semantic-diff.txt")
    build_compression_ratio_graph(semantic_diff_list, False, "Semantic difference compression ratio")
    semantic_merge_list = read_data("semantic-merge.txt")
    build_compression_ratio_graph(semantic_merge_list, True, 'Semantic merge compression ratio')


def build_compression_ratio_graph(semantic_diff_list, reverse: bool, title: str):
    ratios = [stat.after / stat.before for stat in semantic_diff_list if stat.before != 0]
    print(list(filter(lambda x: x.after > x.before, semantic_diff_list)))
    ratios_sorted = sorted(ratios, reverse=reverse)
    mean_ratio = np.mean(ratios_sorted)
    median_ratio = np.median(ratios_sorted)
    q1 = np.percentile(ratios_sorted, 25)
    q3 = np.percentile(ratios_sorted, 75)
    if reverse:
        q1, q3 = q3, q1
    sample_size = len(ratios)  # Размер выборки
    fractions = np.linspace(0, 1, len(ratios_sorted), endpoint=False)
    plt.figure(figsize=(10, 6))
    plt.plot(fractions, ratios_sorted, marker='o', linewidth=0.8, markersize=4)

    plt.axhline(y=mean_ratio, color='r', linestyle='--', alpha=0.7,
                label=f'Mean: {mean_ratio:.2f}')
    plt.axhline(y=median_ratio, color='g', linestyle='--', alpha=0.7,
                label=f'Median: {median_ratio:.2f}')
    plt.axhline(y=q1, color='orange', linestyle=':', alpha=0.7,
                label=f'Q1 (25%): {q1:.2f}')
    plt.axhline(y=q3, color='b', linestyle=':', alpha=0.7,
                label=f'Q3 (75%): {q3:.2f}')
    plt.legend(loc='best', fontsize=12, framealpha=0.9, edgecolor='black')
    plt.xlabel('Fraction of Repositories')
    plt.ylabel('Compression Ratio (after / before)')
    plt.title(f'{title} (n={sample_size})')
    plt.grid(False)
    plt.show()


if __name__ == "__main__":
    main()