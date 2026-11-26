# MCForgeCoinShop (Starter)

Java 17 / Maven project with a GitHub Actions workflow that builds a plugin JAR.

## Build on GitHub from a phone
Upload the **contents** of this ZIP (not the ZIP itself) into your repository root, including `pom.xml`, `src/`, and `.github/`. Commit. Open Actions and download the `MCForgeCoinShop-JAR` artifact after the workflow succeeds.

## Build locally
Run `mvn clean package` with Java 17+ and Maven installed. The JAR is in `target/`.

## Commands
- `/coinshop` opens the category menu
- `/tags` and `/colors` open the configured category
- `/coin balance` (player)
- OP: `/coin give|take|set|check <player> <amount>` and `/coin giveall <amount>`

Items are configured in `src/main/resources/config.yml`, with console commands dispatched after a successful purchase. Replace example delivery commands with commands supported by your installed crate, rank, chat color, or vault plugins.

**Feature limitations:** this is a starter, not the full requested system. Tags/color ownership and applying formatting in chat/TAB, in-game OP editor, editing prices from GUI, and dedicated integrations are not implemented. Built against Paper API 1.21.4; versions 1.21 through 1.26.3 have not been verified, and this build does not guarantee compatibility with every version.
