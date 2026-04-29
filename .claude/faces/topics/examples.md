# Examples

*Version 1.2.1*

Concrete code examples demonstrating best practices from the rules.
All examples use Faces 4.0+ namespaces.
Adapt namespaces and package imports when working with older versions.

## Layout Template

`WEB-INF/templates/layout.xhtml`:
```xml
<!DOCTYPE html>
<html lang="#{facesContext.viewRoot.locale.language}"
    xmlns:h="jakarta.faces.html"
    xmlns:ui="jakarta.faces.facelets"
>
    <h:head>
        <title>#{title}</title>
    </h:head>
    <h:body>
        <header>
            <nav>
                <h:link value="Home" outcome="/index" />
                <h:link value="Employees" outcome="/employees" />
            </nav>
        </header>
        <main>
            <ui:insert name="content" />
        </main>
        <footer>
            <p>Footer content here.</p>
        </footer>
    </h:body>
</html>
```

## Template Client (Page)

`employees.xhtml`:
```xml
<ui:composition template="/WEB-INF/templates/layout.xhtml"
    xmlns:ui="jakarta.faces.facelets"
    xmlns:h="jakarta.faces.html"
    xmlns:f="jakarta.faces.core"
>
    <ui:param name="title" value="Employees" />

    <ui:define name="content">
        <!-- List form: separate UIForm, scoped to datatable actions. -->
        <h:form id="listForm">
            <h:dataTable id="employeesTable" value="#{employeesBacking.employees}" var="employee">
                <h:column>
                    <f:facet name="header">Name</f:facet>
                    #{employee.name}
                </h:column>
                <h:column>
                    <f:facet name="header">Email</f:facet>
                    #{employee.email}
                </h:column>
                <h:column>
                    <h:commandButton id="edit" value="Edit" action="#{employeesBacking.edit(employee)}">
                        <f:ajax render=":editForm" />
                    </h:commandButton>
                </h:column>
            </h:dataTable>
        </h:form>

        <!-- Edit form: separate UIForm, conditionally rendered via always-rendered wrapper. -->
        <h:panelGroup id="editPanel" layout="block">
            <h:form id="editForm" rendered="#{not empty employeesBacking.employee}">
                <h:outputLabel for="name" value="Name:" />
                <h:inputText id="name" value="#{employeesBacking.employee.name}" required="true" />
                <h:message id="nameMessage" for="name" />

                <h:outputLabel for="email" value="Email:" />
                <h:inputText id="email" value="#{employeesBacking.employee.email}" required="true" />
                <h:message id="emailMessage" for="email" />

                <h:commandButton id="save" value="Save" action="#{employeesBacking.save}">
                    <f:ajax execute="@form" render=":editPanel :listForm:employeesTable" />
                </h:commandButton>
                <h:commandButton id="cancel" value="Cancel" action="#{employeesBacking.cancel}">
                    <f:ajax render=":editPanel" />
                </h:commandButton>
    
                <h:messages id="employeeMessages" redisplay="false" />
            </h:form>
        </h:panelGroup>
    </ui:define>
</ui:composition>
```

Key points:
- Template file is in `WEB-INF/templates/` to prevent direct access.
- HTML5 doctype in template, Faces 4.0+ namespaces, no `xmlns="http://www.w3.org/1999/xhtml"`.
- Separate master and detail forms instead of one god form.
- Each `UIInput` has a `UIMessage`.
- Catch-all `UIMessages` with `redisplay="false"` at bottom of edit form.
- Conditionally rendered edit form wrapped in always-rendered `<h:panelGroup id="editPanel">` so ajax can target it.
- `layout="block"` on the wrapper so it renders as `<div>`.
- Fixed IDs matching property/method names: `id="name"` for `value="#{...name}"`, `id="save"` for `action="#{...save}"`.
- Cross-form ajax render uses absolute ID with leading colon: `render=":listForm:employeesTable"`.
- Cancel button uses default of `execute="@this"` to skip processing of input fields.
- Navigation links use `<h:link>` (UIOutcomeTarget), not command components.

## Backing Bean

`EmployeesBacking.java`:
```java
import java.io.Serializable;
import java.util.List;

import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

@Named
@ViewScoped
public class EmployeesBacking implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private EmployeeService employeeService;

    private List<Employee> employees;
    private Employee employee;

    @PostConstruct
    public void init() {
        employees = employeeService.list();
    }

    public void edit(Employee employee) {
        this.employee = employee;
    }

    public void save() {
        employeeService.save(employee);
        employee = null;
        employees = employeeService.list();
    }

    public void cancel() {
        employee = null;
    }

    public List<Employee> getEmployees() {
        return employees;
    }

    public Employee getEmployee() {
        return employee;
    }

    public void setEmployee(Employee employee) {
        this.employee = employee;
    }
}
```

Key points:
- `@Named` + `@ViewScoped` from `jakarta.faces.view`, not `jakarta.enterprise.context`.
- Implements `Serializable` because `@ViewScoped` is stored in `HttpSession`.
- Initial data loaded in `@PostConstruct`, not in constructor or getter.
- Getters are pure; `edit()` is an action method, `save()` is an action method.
- Service injected with `@Inject`, not `@ManagedProperty`.

## Include with ui:param

`WEB-INF/includes/status-badge.xhtml`:
```xml
<ui:composition
    xmlns:h="jakarta.faces.html"
    xmlns:ui="jakarta.faces.facelets"
>
    <h:outputText value="#{label}" styleClass="badge badge-#{type}" />
</ui:composition>
```

Usage in a page:
```xml
<ui:include src="/WEB-INF/includes/status-badge.xhtml">
    <ui:param name="label" value="#{employee.status}" />
    <ui:param name="type" value="#{employee.active ? 'success' : 'danger'}" />
</ui:include>
```

Key points:
- Include file is in `WEB-INF/includes/` to prevent direct access.
- Two `<ui:param>` instances; if more than two, consider converting to a tag file.

## Tag File

`WEB-INF/tags/field.xhtml` (registered via taglib with namespace `xmlns:t="tags"`):
```xml
<ui:composition
    xmlns:h="jakarta.faces.html"
    xmlns:ui="jakarta.faces.facelets"
>
    <div class="field">
        <h:outputLabel for="#{id}" value="#{label}" />
        <h:inputText id="#{id}" value="#{value}" required="#{required}" />
        <h:message id="#{id}Message" for="#{id}" />
    </div>
</ui:composition>
```

Usage in a page:
```xml
<t:field id="name" label="Name:" value="#{employeesBacking.employee.name}" required="true" />
<t:field id="email" label="Email:" value="#{employeesBacking.employee.email}" required="true" />
```

Key points:
- Tag file is in `WEB-INF/tags/` to prevent direct access.
- Registered via `tags.taglib.xml` with a custom namespace prefix.
- Encapsulates label + input + message pattern into a reusable tag.
- Three or more parameters, so a tag file is preferred over an include with `<ui:param>`.

## Composite Component

`WEB-INF/resources/components/confirmButton.xhtml`:
```xml
<ui:component
    xmlns:h="jakarta.faces.html"
    xmlns:f="jakarta.faces.core"
    xmlns:ui="jakarta.faces.facelets"
    xmlns:cc="jakarta.faces.composite"
>
    <cc:interface>
        <cc:attribute name="value" type="java.lang.String" required="true" />
        <cc:attribute name="confirmMessage" type="java.lang.String" default="Are you sure?" />
        <cc:actionSource name="action" targets="confirm" />
    </cc:interface>

    <cc:implementation>
        <span id="#{cc.clientId}">
            <h:commandButton id="confirm" value="#{cc.attrs.value}" onclick="return confirm('#{cc.attrs.confirmMessage}')">
                <f:ajax execute="@form" render="@form" />
            </h:commandButton>
        </span>
    </cc:implementation>
</ui:component>
```

Usage in a page (with taglib namespace `xmlns:t="tags"`):
```xml
<t:confirmButton id="delete" value="Delete" confirmMessage="Delete this employee?">
    <f:actionListener for="action" binding="#{employeesBacking.delete(employee)}" />
</t:confirmButton>
```

Key points:
- Composite is in `WEB-INF/resources/components/` to prevent direct access to source.
- Uses `<cc:actionSource>` with `targets` to expose the inner button's action to the client.
- The composite is a `NamingContainer`; IDs inside are automatically scoped.
- The `<span id="#{cc.clientId}">` (or `<div>` for block-level content) wrapper is a best practice to make the composite ajax-updatable by just its ID; without it, `render=":compositeId"` has no HTML element to update.
- Registered via `<composite-library-name>components</composite-library-name>` in `tags.taglib.xml`.

## GET Search Form (f:metadata, f:viewParam, f:viewAction)

`employees.xhtml`:
```xml
<ui:composition template="/WEB-INF/templates/layout.xhtml"
    xmlns:ui="jakarta.faces.facelets"
    xmlns:h="jakarta.faces.html"
    xmlns:f="jakarta.faces.core"
>
    <ui:param name="title" value="Search Employees" />

    <f:metadata>
        <f:viewParam name="query" value="#{employeesSearchBacking.query}" />
        <f:viewParam name="department" value="#{employeesSearchBacking.department}" />
        <f:viewAction action="#{employeesSearchBacking.search}" />
    </f:metadata>

    <ui:define name="content">
        <!-- GET search form: plain HTML <form>, NOT <h:form>. -->
        <!-- Input names must match the <f:viewParam> names so the bookmarkable URL is "?query=...&department=...". -->
        <form>
            <label for="query">Search:</label>
            <input id="query" name="query" type="text" value="#{employeesSearchBacking.query}" />

            <label for="department">Department:</label>
            <select id="department" name="department">
                <option value="">All</option>
                <ui:repeat value="#{employeesSearchBacking.departments}" var="department">
                    <option value="#{department}" selected="#{department eq employeesSearchBacking.department ? 'selected' : null}">#{department}</option>
                </ui:repeat>
            </select>

            <button type="submit">Search</button>
        </form>

        <h:dataTable value="#{employeesSearchBacking.results}" var="employee" rendered="#{not empty employeesSearchBacking.results}">
            <h:column>
                <f:facet name="header">Name</f:facet>
                #{employee.name}
            </h:column>
            <h:column>
                <f:facet name="header">Department</f:facet>
                #{employee.department}
            </h:column>
        </h:dataTable>

        <h:outputText value="No results." rendered="#{employeesSearchBacking.searched and empty employeesSearchBacking.results}" />
    </ui:define>
</ui:composition>
```

`EmployeesSearchBacking.java`:
```java
import java.util.List;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

@Named
@RequestScoped
public class EmployeesSearchBacking {

    @Inject
    private EmployeeService employeeService;

    private String query;
    private String department;
    private List<String> departments;
    private List<Employee> results;
    private boolean searched;

    @PostConstruct
    public void init() {
        departments = employeeService.listDepartments();
    }

    public void search() {
        if (query != null || department != null) {
            results = employeeService.search(query, department);
            searched = true;
        }
    }

    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public List<String> getDepartments() { return departments; }
    public List<Employee> getResults() { return results; }
    public boolean isSearched() { return searched; }
}
```

Key points:
- `<f:metadata>` is the FIRST direct child of the composition root (template client), not inside the master template.
- `<f:viewParam>` extracts each query string parameter, converts/validates it, and pushes it into the bean during Apply Request Values, Process Validations, and Update Model Values — exactly like a `UIInput` on a postback.
- `<f:viewAction>` invokes `search()` during Invoke Application, AFTER the `<f:viewParam>` values are applied — so `query` and `department` are already populated; `@PostConstruct` would run too early for this.
- This is a GET form, so it MUST be a plain HTML `<form>`, NOT `<h:form>` (which is POST). UIInput components inside a non-UIForm container would render with client IDs that do not match the `<f:viewParam>` names; using plain `<input name="...">` keeps the URL clean and bookmarkable.
- `value="#{employeesSearchBacking.query}"` on the plain inputs preserves the previous search terms when the page is reloaded with the same URL.
- The result URL `?query=foo&department=Sales` is bookmarkable, shareable, and reproducible.
- Caveat: this GET request runs the FULL lifecycle (not just Restore View + Render Response), since `<f:viewParam>` and `<f:viewAction>` are present.
- Bean is `@RequestScoped`: there is no `<h:form>` (no postback), so view-scope state is not needed; each search is a fresh GET request that re-applies the bookmarkable parameters and re-runs `<f:viewAction>`. No `Serializable` needed either.

## POST-Redirect-GET Navigation

Action method returning navigation outcome with redirect:
```java
public String save() {
    employeeService.save(employee);
    return "/employees?faces-redirect=true";
}
```

For navigation without a business action, use `<h:link>` or `<h:button>` instead:
```xml
<h:link value="View Employee" outcome="/employee">
    <f:param name="id" value="#{employee.id}" />
</h:link>
```

Key points:
- `?faces-redirect=true` triggers a 302 redirect after POST, preventing double-submit on refresh.
- `<h:link>` renders an `<a>` tag with a GET URL; no form submission needed.
- `<f:param>` passes query parameters in the URL.

## Conditional Rendering with JSTL vs rendered

WRONG — using JSTL to conditionally render:
```xml
<!-- DON'T: JSTL is build-time, not render-time; whilst this might work, it might instantiate the bean at the wrong/unexpected moment. -->
<c:if test="#{bean.showPanel}">
    <h:panelGroup id="panel">...</h:panelGroup>
</c:if>
```

CORRECT — using `rendered` attribute:
```xml
<h:panelGroup id="panel" rendered="#{bean.showPanel}">
    ...
</h:panelGroup>
```

CORRECT — using JSTL to dynamically build the component tree:
```xml
<!-- OK: JSTL used at build-time to select which component to include. -->
<c:choose>
    <c:when test="#{field.type == 'TEXT'}">
        <h:inputText id="input" value="#{bean.value}" />
    </c:when>
    <c:when test="#{field.type == 'TEXTAREA'}">
        <h:inputTextarea id="input" value="#{bean.value}" />
    </c:when>
    <c:when test="#{field.type == 'PASSWORD'}">
        <h:inputSecret id="input" value="#{bean.value}" />
    </c:when>
</c:choose>
```
