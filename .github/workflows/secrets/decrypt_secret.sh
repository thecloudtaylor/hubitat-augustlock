#!/bin/sh

# Decrypt the file
mkdir $HOME/secrets
# --batch to prevent interactive command
# --yes to assume "yes" for questions
gpg --quiet --batch --yes --decrypt --passphrase="$ENCRYPTION_KEY" \
--output $HOME/secrets/id_rsa ./.github/workflows/secrets/package_repo.enc