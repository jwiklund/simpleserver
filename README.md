Note to self: Use devd instead https://github.com/cortesi/devd

A simple http file server with proxy support.
Will serve all files in the current directory, or the directory given as argument on the commandline as well as proxy requests listed in server.conf.

Nb, if the commandline argument is a zip file the contents of the zip file will be served.

Eg, to proxy /google to www.google.com add the following in server.conf.

```yaml
proxy_google: "https://www.google.com"
```
