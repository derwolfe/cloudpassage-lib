# The CloudPassage API #

Most information has been referenced from:
https://support.cloudpassage.com/entries/23110911-Overview

## API Client ##

1. First, we need to get a client ID and secret key. These *must* be obtained
   through the CloudPassage UI. These must be provided with all API requests.

   To retrieve your client credentials, access the Halo Portal web interface
   and navigate to [Settings menu] > Site Administration > API Keys. If you
   administer multiple accounts, you will have to access each account
   separately to obtain a client ID and secret for each one.

2. Using the key and ID (which don't change), we'll next obtain a token for
   our API requests.

   Send the client ID and client secret in the "Authorization" header of the
   POST request. Construct the header as follows:

   - Combine the client ID and client secret into a string
   `"client_id:client_secret"` (with a colon seprating the two elements).

   - Encode the resulting string using Base64.

   - Construct the "Authorization" header value by specifying the authorization
     method followed by a space, followed by the encoded string. For example:

     ```
     Authorization: Basic aGFsbzpjbG91ZHBhc3NhZ2U=
     ```

   - If the request is valid, the authorization server will issue an access
     token. The response also includes an expiration timeout (`expires_in`) for
     the access token, expressed in seconds. This is an example response:

     ```
     POST https://API.cloudpassage.com/oauth/access_token?grant_type=client_credentials

     ...

     HTTP/1.1 200 OK
     Content-Type: application/json;charset=UTF=8
     Cache-Control: no-store
     Pragma: no-cache

     {
         "access_token":"ffad76cc550110fc4c84a18397b6e104",
         "token_type":"bearer",
         "expires_in":900
     }
     ```

     In this library, we save the token in Redis.

3. When making a call to the API, pass the access token in the Authorization
   header field. Specify the authentication scheme as "Bearer", followed by a
   space and then the token. For example:

   ```
   Authorization: Bearer ffad76cc550110fc4c84a18397b6e104
   ```
