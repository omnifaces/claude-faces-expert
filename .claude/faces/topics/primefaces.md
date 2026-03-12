# PrimeFaces

*Version 1.1.0*

PrimeFaces is the most widely used component library for Jakarta Faces. When this project includes PrimeFaces, follow these rules.

## General

- PrimeFaces `UICommand` (e.g.`<p:commandButton>`) is ajax by default, unlike standard Faces `UICommand` (e.g.`<h:commandButton>`).
- Use `process` and `update` attributes (PrimeFaces names), not `execute` and `render` (standard names).
- For lazy-loading datatables: implement `LazyDataModel<T>` with `count()` and `load()` methods.
- As catch-all `UIMessages`, prefer `<p:growl>` or `<p:messages>` with `redisplay="false"`.
- For file downloads with ajax, use `<p:fileDownload>` which supports ajax since PrimeFaces 10; alternatively, disable ajax with `ajax="false"` on the `UICommand`.

## PrimeFaces Selectors

PrimeFaces Selectors `@(.css-class)` are very useful when you need to ajax-update multiple components on a particular event, you can create a marker class like `updateOnChangeFoo` and have `<p:inputText value="#{bean.foo}"><p:ajax update="@(.updateOnChangeFoo)"></p:inputText>` and add the marker class to the desired target components `<h:outputPanel id="somePanel" styleClass="updateOnChangeFoo">`.
The only condition is that the target component MUST have a ID set.

## Head Resources

NEVER use `<f:facet name="first">`, `<f:facet name="middle">`, or `<f:facet name="last">` inside `<h:head>`.
These are PrimeFaces-specific `HeadRenderer` extensions and are incompatible with other libraries (e.g. OmniFaces `CombinedResourceHandler`).
Instead, place any `<h:outputScript>` or `<h:outputStylesheet>` that must override library-provided resources inside `<h:body>` with `target="head"`.
These will automatically be rendered as the last entries in `<h:head>`.

## Dialogs

`<p:dialog>` (and similar overlay components) MUST have its own `UIForm` inside it, not be placed inside an outer form.
PrimeFaces appends dialogs to the end of the HTML `<body>`, so they end up outside any outer form in the DOM.

## CSS

ALWAYS load the CSS file with custom definitions via a `<h:outputStylesheet target="head">` inside `<h:body>`.
When overriding PrimeFaces-specific CSS, ALWAYS use the exact same selector PrimeFaces is using, and use a separate CSS file from user-defined classes, e.g. `primefaces-overrides.css`.

## References

- Source code: https://github.com/primefaces/primefaces
- Java API: https://javadoc.io/doc/org.primefaces/primefaces/latest/index.html
- VDL (tag docs): https://primefaces.github.io/primefaces/vdldoc/
- Showcase with examples: https://showcase.primefaces.org
