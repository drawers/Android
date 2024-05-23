# Automated migrations using lint and LM

This is a fork of the DuckDuckGo Android project. We chose this project to experiment on for two reasons:
1. It's public so we don't have to worry about jeopardizing intellectual property
2. It's representative of a large Android project
3. It already has integration with lint, so this makes it easier to try out new things.

Look at the pull requests:

https://github.com/drawers/Android/pull/2
https://github.com/drawers/Android/pull/3

They are the result of including some scaffolding (lint rule and integration with some LM) and running `./gradlew lintFix --continue` to produce

## License
DuckDuckGo android is distributed under the Apache 2.0 [license](LICENSE).
