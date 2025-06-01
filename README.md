## About

This tool was designed to gather statistics about merge conflicts and differences in Git repositories.

## Usage

`MainKt` - entrypoint, it should be run with the program arguments. Alternatively, program can be run with the IDEA `Run Search` configuration.

## Arguments
`-p, --path` - specify path of the git repository in the local machine. Example: `java MainKt --path="/some/path"`

`-u, --url` - specify url to the git repository. Example: `java MainKt --url="https://github.com/gradle/gradle.git"`

`--include-base, -b` - whether contents of the base commit should be included in the file with conflicts

`--group-filetype, -g` - whether should group conflicts by filetype or not

`--no-cache` - remove remote repository content after processing merged commits.

`--filter, -f` - filter filenames by given regular expression satisfying PCRE. Example `java MainKt --filter="*.properties"`

`--multiple-repo-path` - Local path to the list of repositories with files. The option is used when inspector should process multiple repositories. Example: `java MainKt --multiple-repo-path="/some/path/to/file"`

## Statistics values

Merge statistics (per repository):
1. Number of times the file had a conflict.
2. Number of conflicting chunks. One conflict may contain multiple chunks.

Difference statistics:
1. Number of times the file has been modified.
2. Number of modification hunks.
3. Number of deletion hunks.
4. Number of addition hunks.

## Data for semantic approaches for properties files
1. Data from the proof of relevance phase can be found by the [link](https://drive.google.com/drive/folders/1q2NYtJ-xkPx4k870JgilKz_2pqsGk_Ru?usp=sharing).
2. Results from semantic difference benchmark can be found in file `search/semantic-diff.txt`.
3. Results from semantic merge benchmark can be found in file `search/semantic-merge.txt`.