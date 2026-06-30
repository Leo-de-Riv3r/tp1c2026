#!/bin/sh
set -e
npm run seed:users
exec npm start
