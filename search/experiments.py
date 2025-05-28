import statistics
if __name__ == "__main__":
    diff = statistics.read_data("semantic-merge.txt")
    print('git conflict:', sum([i.before for i in diff]))
    print('Semantic resolved conflict:', sum([i.after for i in diff]))
    conflicts = [i for i in diff if i.after == 0 and i.before != 0]
    print(len(conflicts))
