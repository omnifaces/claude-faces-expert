# Minimal Faces Configuration

*Version 1.1.0*

## web.xml

`WEB-INF/web.xml` MUST minimally have this configuration:
```xml
<context-param>
    <param-name>jakarta.faces.FACELETS_LIBRARIES</param-name>
    <param-value>/WEB-INF/tags.taglib.xml</param-value> <!-- To register custom namespace with custom tag files. -->
</context-param>
<context-param>
    <param-name>jakarta.faces.FACELETS_SKIP_COMMENTS</param-name>
    <param-value>true</param-value> <!-- To prevent EL expressions inside comments from being evaluated. -->
</context-param>
<context-param>
    <param-name>jakarta.faces.INTERPRET_EMPTY_STRING_SUBMITTED_VALUES_AS_NULL</param-name>
    <param-value>true</param-value> <!-- To prevent empty string submitted values slipping through @NotNull validation and/or ending up in database. -->
</context-param>
<context-param>
    <param-name>jakarta.faces.PROJECT_STAGE</param-name>
    <param-value>Development</param-value> <!-- To disable XHTML cache so it can be hot-reloaded, and to get development stage messages; NOT FOR PRODUCTION! -->
</context-param>
<context-param>
    <param-name>jakarta.faces.WEBAPP_RESOURCES_DIRECTORY</param-name>
    <param-value>WEB-INF/resources</param-value> <!-- To prevent direct access to composite component source code. -->
</context-param>

<servlet>
    <servlet-name>facesServlet</servlet-name>
    <servlet-class>jakarta.faces.webapp.FacesServlet</servlet-class>
    <load-on-startup>1</load-on-startup>
</servlet>
<servlet-mapping>
    <servlet-name>facesServlet</servlet-name>
    <url-pattern>*.xhtml</url-pattern> <!-- Explicitly register single `*.xhtml` URL pattern to prevent implicit ones such as /faces/*, *.faces, and *.jsf from being activated. -->
</servlet-mapping>
```

## faces-config.xml

`WEB-INF/faces-config.xml` MUST minimally have this configuration:
```xml
<application>
    <locale-config>
        <default-locale>en</default-locale> <!-- Set to your default locale. -->
    </locale-config>
</application>
```

## tags.taglib.xml

The taglib file name prefix is free to choose (e.g. `tags.taglib.xml`, `app.taglib.xml`, `myproject.taglib.xml`).
Only the `.taglib.xml` suffix is required by convention.
The name must match the value in `jakarta.faces.FACELETS_LIBRARIES` above.

The taglib file MUST minimally have this configuration:
```xml
<facelet-taglib>
    <namespace>tags</namespace>
    <short-name>t</short-name>
    <composite-library-name>components</composite-library-name>
</facelet-taglib>
```
So that all tags AND composites can be referenced via the same XML namespace `xmlns:t="tags"`.

## Directory Structure

All assets/templates/includes/tagfiles/composites MUST be placed inside `WEB-INF` folder to prevent direct access by client.
```
|-- WEB-INF/
|    |-- includes/
|    |-- resources/
|    |    |-- components/
|    |    |    `-- composite.xhtml
|    |    |-- fonts/
|    |    |    `-- font.woff2
|    |    |-- icons/
|    |    |    `-- icon.svg
|    |    |-- images/
|    |    |    `-- image.png
|    |    |-- scripts/
|    |    |    `-- script.js
|    |    `-- styles/
|    |    |    `-- style.css
|    |-- tags/
|    |    `-- tagfile.xhtml
|    |-- templates/
|    |    `-- template.xhtml
|    |-- beans.xml
|    |-- faces-config.xml
|    |-- tags.taglib.xml
|    `-- web.xml
`-- page.xhtml
```
