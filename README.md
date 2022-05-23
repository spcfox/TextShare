# TextShare

Kotlin backend application for sharing texts.

## API

All correctly processed requests are returned with a 200 code.
If the request is successful, the response is
> {"ok": true, "result": RESULT}

If an error occurs, the response is
> {"ok": false, "error": ERROR_NAME, "message": ERROR_MESSAGE}

### Methods

#### Account info
> GET /account/info

Returns account information.
* **token** — access token

#### Create account
> POST /account/create

Creates an account and returns access token.
* **name** — account name

#### Edit account
> POST /account/edit

Edit account and returns account information.
* **token** — access token
* **name** — account name

#### Revoke token
> POST /account/revoke

Revoke token and returns new token.
* **token** — access token

#### Get text

> GET /text/{textId}

Returns text.
* **token** *(Optional)* —
access token to see if you can edit with this token.
* **textId** — text identifier.

#### Get text list

> GET /text/list

Returns the latest public texts with pagination.
* **token** *(Optional)* —
  access token to see if you can edit with this token.
* **page** *(Optional)* — page number. Default 0.
* **pageSize** *(Optional)* — number of texts per page. Default 20.

#### Get user text list

> GET /text/user-list

Returns all user texts with pagination.
* **token** — access token.
* **page** *(Optional)* — page number. Default 0.
* **pageSize** *(Optional)* — number of texts per page. Default 20.

#### Text create

> POST /text/create

Create text and returns text id.
* **token** — access token.
* **title** — text title.
* **exposure** — text exposure.
* **request body** — content.

#### Text edit

> POST /text/edit/{textId}

Edit text and returns text id.
* **token** — access token.
* **textId** — text identifier.
* **title** *(Optional)* — text title.
* **exposure** *(Optional)* — text exposure.
* **request body** *(Optional)* — content.

#### Text delete

> POST /text/delete/{textId}

Delete text and returns text id.
* **token** — access token.
* **textId** — text identifier.


### Entities

#### Account

* **userId** — account identifier.
* **name** — the username used to display as the name of the author of the text.

#### Text Exposure

Enumerating text permissions:
* **PUBLIC** — everyone can see the text.
* **UNLISTED** — the text is available to everyone, but only through a direct link.
* **PRIVATE** — only author can see the text.

#### Text

* **textId** — text identifier.
* **title** — text title.
* **body** — text content.
* **author** — name of author.
* **exposure** — text exposure.
* **createdAt** — date of creation of the text.
* **editedAt** — date the text was last edited.
* **canEdit** — true if the text can be edited with the passed token.

### Errors

* **INVALID_TOKEN** — invalid token passed.
* **PERMISSION_DENIED** — not enough permissions  to perform the action
* **INVALID_NAME** — invalid name passed.
* **INVALID_TEXT** — invalid title or body passed.
* **TEXT_NOT_FOUND** — text with specified id not found.