# skript-migrate

Create migration file and share them to your users. It able them to migrate to the last version in one command!

## Creating your first migration

A migration file looks like this:

```yaml
author: Olyno
addon_name: Skent
steps:
  2.2.0:
    - find: rename (.+) with replace
      result: rename $1 by replacing existing ones
    - find: test
      result: Final test!
```

Once you have created your migration file, and placed it in the ``plugins/SkriptMigrate/migrations`` folder, you just need to run the ``/migrate`` command to start the migrations.

## Configuration

Here is a list of available options:

```yaml
author: Your author name (string) (optional)
addon_name: Your addon name (string) (not case sensitive, not optional)
steps: All steps executed during the migration (list) (not optional)
```

Each step contains a version which is your addon version. Each version contains infinite steps that is executed in the order (from bottom for old steps to top for new ones). Each step can be configured as:

```yaml
find: A regex that match any line of script. (string)
result: What the result should look like. (string)
```

⚠️ Versions must be in the order from latest at the top to the old one at the bottom

## License

Code released under GNU GPLv3 license.

Copyright ©, [Olyno](https://github.com/Olyno).