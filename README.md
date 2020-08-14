# delete-duplicate-emails

This library can be used for 2 email accounts where the source account was used to forward emails to the destination account which should now be deleted in the destination account and only remain in the source account.

## Disclaimer

All the functions above do work but are brittle due to the fact that no error handling is implemented and furthermore the clojure-mail dependency is using an older version of JavaMail, upgrading it and implementing proper error handling like an adult might be advisable if anyone wants to use this base for something more serious.

## Usage

Fill in your credentials for the source and destination account in `src/delete_duplicate_emails/config.clj`.
Then fire up your REPL and invoke `(load-messages-from-source)` to iterate through the emails in the source account and save some of their metadata into the local CRUX instance.
Finally to delete the duplicate emails in the destination account run `(remove-duplicates-from-destination)`.

## License

Copyright © 2020 Philipp Küng

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
