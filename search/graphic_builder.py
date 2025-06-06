import matplotlib.pyplot as plt
import numpy as np

from statistics import read_data


def main():
    semantic_diff_list = read_data("semantic-diff.txt")
    build_compression_ratio_graph(semantic_diff_list, False, r'Cumulative distribution plot of the highlighting \textit{Compression Ratio}', 'CR', r'$CR_{repository} = \frac{LOC_{SemanticHighlighting}}{LOC_{DefaultHighlighting}}$', 0.1, 0.525)
    semantic_merge_list = read_data("semantic-merge.txt")
    build_compression_ratio_graph(semantic_merge_list, True, r'Cumulative distribution plot of the \textit{Automatic Conflict Resolution Ratio}', 'ACCR', r'$ACRR_{repository} = \frac{|ResolvableConflicts|}{|AllConflicts|}$', 0.58, 0.47)


def build_compression_ratio_graph(statistics_list, reverse: bool, title: str, y_label: str, metric: str, ypos: float, xpos: float):
    plt.rcParams['text.usetex'] = True
    plt.rcParams['font.family'] = 'sans-serif'
    ratios = [stat.after / stat.before for stat in statistics_list if stat.before != 0]
    ratios_sorted = sorted(ratios, reverse=reverse)
    mean_ratio = np.mean(ratios_sorted)
    median_ratio = np.median(ratios_sorted)
    q1 = np.percentile(ratios_sorted, 25)
    q3 = np.percentile(ratios_sorted, 75)
    if reverse:
        q1, q3 = q3, q1
    fractions = np.arange(0, len(ratios_sorted))
    sample_size = len(ratios)
    plt.figure(figsize=(10, 6))
    plt.plot(fractions, ratios_sorted, marker='o', linewidth=0.8, markersize=6)

    plt.axhline(y=mean_ratio, color='r', linestyle='--', alpha=0.7, linewidth=3.0,
                label=f'Mean: {mean_ratio:.2f}')
    plt.axhline(y=median_ratio, color='g', linestyle='--', alpha=0.7, linewidth=3.0,
                label=f'Median: {median_ratio:.2f}')
    plt.axhline(y=q1, color='orange', linestyle=':', alpha=0.7, linewidth=3.0,
                label=f'Q1 (25\\%): {q1:.2f}')
    plt.axhline(y=q3, color='black', linestyle=':', alpha=0.7, linewidth=3.0,
                label=f'Q3 (75\\%): {q3:.2f}')
    plt.legend(loc='best', fontsize=16, framealpha=0.9, edgecolor='black')
    plt.xlabel('Number of Repositories', fontsize=18)
    plt.ylabel(y_label, fontsize=18)

    plt.text(xpos, ypos, metric,
             transform=plt.gca().transAxes,
             fontsize=20,
             bbox=dict(boxstyle='round', facecolor='white', alpha=0.9, edgecolor='gray'))
    plt.title(f'{title} (n={sample_size})', fontsize=20)
    plt.xticks(fontsize=14)
    plt.yticks(fontsize=14)
    plt.grid(False)
    plt.show()


if __name__ == "__main__":
    main()