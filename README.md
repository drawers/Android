# DuckDuckGo Android

This is a fork of the DuckDuckGo Android project which is merely being used as an example of a large Android codebase.

I am experimenting with performing an automated refactor using a combination of a language model and lint.

The refactor is a renaming of test cases. It doesn't reflect my personal preference for test names, it's merely
an example of some automated refactor that would not have been achievable prior to the general availability of 
language models like GPT-3.

Here, we're using lint (rule based) to focus in on just the data we want to change. Then we're using the fuzziness of an LLM
to achieve something that is not possible using rule-based engines - formulation of meaning.

This isn't a new concept - Moderne and OpenRewrite are thinking in this way too and there are talks at DroidCon SF 2024 in this vein
as well.

## Building the Project
We use git submodules and so when you are checking out the app, you'll need to ensure the submodules are initialized properly. You can use the `--recursive` flag when cloning the project to do this.

    git clone --recursive https://github.com/duckduckgo/android.git

Alternatively, if you already have the project checked out, you can initialize the submodules manually.

    git submodule update --init

## Contribute

Please refer to [contributing](CONTRIBUTING.md).

## License
DuckDuckGo android is distributed under the Apache 2.0 [license](LICENSE).
