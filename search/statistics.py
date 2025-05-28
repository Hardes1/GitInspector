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