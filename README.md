A simple http file server with proxy support.
Will proxy all files in the current directory as well as proxy requests listed in server.conf.

Eg, to proxy /google to www.google.com add the following in server.conf.

```yaml
proxy_google: "https://www.google.com"
```
