import statistics

def diff_total():
    diff =  statistics.read_data("semantic-diff.txt")
    print('Regular highlighting:', sum([i.before for i in diff]))
    print('Semantic diff highlighting:', sum([i.after for i in diff]))


def merge_total():
    merge = statistics.read_data("semantic-merge.txt")
    print('Git conflict:', sum([i.before for i in merge]))
    print('Semantic resolved conflict:', sum([i.after for i in merge]))
    conflicts = [i for i in merge if i.after == 0 and i.before != 0]
    print(len(conflicts))

if __name__ == "__main__":
    diff_total()
