# How to contribute

## We welcome contributions from the public. However there are some guidelines to follow:

1. We cannot accept new language versions and translations from community, this is because:
	- Some expressions used within the app are strictly coupled with Finnish law and thus must meet it's requirements
	- There's also a lot of domain specific terminology involved which must be handled by THL's board of experts
	- Small typo fixes can be processed manually but better way to report those is to raise an issue. We can then manually credit changes to issue's original author
2. Prior to committing to work, **create an issue** describing a problem, bug or enhancement you would to like to work on
3. **Wait for discussion** on the issue and how it should be solved
4. **Wait for main contributors** of the repository to handle the issue and clear it for implementation
5. Embrace the feedback and be patient. We are working as fast as we can to improve Koronavilkku and community's help is much appreciated

## Issues

The issue tracker is the preferred channel for reports and queries.

## Pull requests

Our team will review the pull requests and contributors will get credit. You can see all contributors in AUTHORS.md.

## Commit message styleguide

We follow [The seven rules of a great Git commit message](https://chris.beams.io/posts/git-commit/).

### TL;DR

- Title line max 50 characters
- Single empty line between title and detailed description when description is needed
- Explain what and why vs. how
- Use the imperative mood in the title line
  - :heavy_check_mark: Refactor subsystem X for readability
  - :heavy_check_mark: Update getting started documentation
  - :heavy_check_mark: Remove deprecated methods
  - :heavy_check_mark: Release version 1.0.0
  - :x: Fixed bug with Y
  - :x: Changing behavior of X
  - :x: More fixes for broken stuff
- Attach issue tracker references at bottom after single empty line `Resolves: #123` or `See also: #456, #789`
