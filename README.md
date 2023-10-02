# deps-diff

A tool for comparing transitive dependencies in two deps.edn files.

Certainly, the Clojure ecosystem does not strictly follow SemVer ([Spec-ulation](https://www.youtube.com/watch?v=oyLBGkS5ICk&t=2789s)),
and if there are no concrete benefits, it is recommended not to update dependencies.
However, when it becomes necessary to replace an artifact, you need to be extremely careful,
especially when implicit transitive dependencies change, as compatibility issues may arise.

For example, consider the following dependency tree:

```
+-----+     +--------+     +--------+
|  A  | --> | B(1.0) | --> | C(1.0) |
+-----+     +--------+     +--------+
      \     +--------+     +--------+
       +--> | D(1.0) | --> | C(1.0) |
            +--------+     +--------+
```

Let's assume a situation where we need to update dependency B:

```
+-----+     +--------+     +--------+
|  A  | --> | B(2.0) | --> | C(2.0) |
+-----+     +--------+     +--------+
      \     +--------+     +--------+
       +--> | D(1.0) | --> | C(1.0) |
            +--------+     +--------+
```

In this scenario, B internally updated C to 2.0. If we haven't explicitly specified the version of C,
there is no way to guarantee that D will work correctly (See [Dep selection](https://clojure.org/reference/dep_expansion#_dep_selection)).

But it's good to know that such potential risks can be detected in advance.
`deps-diff` is a GitHub Action created for this purpose.


### Inputs

| Name        | Description                                                                                                                                     | Default Value              |
|-------------|-------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------|
| `base`      | The deps.edn before the change being referenced. You can specify a git ref or file path. The default value is the git ref of the base branch of the PR, referencing the `deps.edn` at the repository's root path. You can specify it like `{{git-ref}}:{{path-to-deps.edn}}`. | Git ref of PR's base branch |
| `target`    | The deps.edn after the change being referenced. You can specify a git ref or file path. The default value is the deps.edn in the current directory. | `deps.edn` in the current directory |
| `format`    | Determines the format of the output. You can specify `edn`, `markdown`, or `cli`. The default value is edn | `edn` |
| `aliases`   | Specifies the aliases to be used when forming the basis. It must be expressed as a quoted sequence (e.g., `'[:dev :test]'`). | `nil` |


### Outputs

- `deps_diff` - The name of the outlet where the execution result is output. Use it along with the action's id in your workflow.

### Use in GitHub Workflow

Example:

```yml
name: Notify dependency diff

on:
  workflow_dispatch:
  pull_request:
    paths:
      - 'deps.edn'

jobs:
  notify:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
        with:
          fetch-depth: 0 # Required to make it possible to compare with PR base branch

      - name: Diff dependencies
        id: diff
        uses: namenu/deps-diff@main
        with:
          format: markdown

      - uses: marocchino/sticky-pull-request-comment@v2
        # # An empty diff result will break this action.
        # if: ${{ steps.composer_diff.outputs.composer_diff_exit_code != 0 }}
        with:
          header: deps-diff # Creates a collapsed comment with the report
          message: |
            ### deps.edn changes

            ${{ steps.diff.outputs.deps_diff }}
```