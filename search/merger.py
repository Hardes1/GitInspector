def merge_files(file_a, file_b, output_file):
    unique_lines = set()

    with open(file_a, 'r') as file_a_handle:
        for line in file_a_handle:
            unique_lines.add(line.strip())

    # Read lines from file_b and add to the set
    with open(file_b, 'r') as file_b_handle:
        for line in file_b_handle:
            unique_lines.add(line.strip())

    # Write unique lines to the output file
    with open(output_file, 'w') as output_file_handle:
        for line in sorted(unique_lines):
            output_file_handle.write(line + '\n')


file_a = 'repositories-github.txt'
file_b = 'repositories-bigcode.txt'
file_c = 'repositories-merged.txt'

merge_files(file_a, file_b, file_c)

