# openHAB Alexa Configuration

The openHAB Alexa Configuration binding allows fined grained control over which items are exposed to the Alexa service.  By default, or without this binding being installed, all items are exposed to Alexa.  Alexa will add any items that have that have compatible tags such as "Lighting" or "Switchable".  Users may use this binding to restrict which items are exposed. 


## Configuration

After installing this add-on, you will find configuration options in the Paper UI under _Configuration->Services->IO->openHAB Alexa_:


Alternatively, you can configure the settings in the file `conf/services/alexa.cfg`:

```
############################## openHAB Alexa Binding #############################


# Expose all items to Alexa
# Possible values are:
# - true : Expose all item to Alexa
# - false: Only expose selected items to Alexa
# Optional, default is 'true'.
#expose-all=

# A comma-separated list of items to beto expose to Alexa.
# Optional, default is an empty list.
#exposed-items=
```

