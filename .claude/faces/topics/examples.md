# Examples

*Version 1.0.0*

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
        <h:commandButton id="confirm" value="#{cc.attrs.value}" onclick="return confirm('#{cc.attrs.confirmMessage}')">
            <f:ajax execute="@form" render="@form" />
        </h:commandButton>
    </cc:implementation>
</ui:component>
```

Usage in a page (with taglib namespace `xmlns:t="tags"`):
```xml
<t:confirmButton value="Delete" confirmMessage="Delete this employee?">
    <f:actionListener for="action" binding="#{employeesBacking.delete(employee)}" />
</t:confirmButton>
```

Key points:
- Composite is in `WEB-INF/resources/components/` to prevent direct access to source.
- Uses `<cc:actionSource>` with `targets` to expose the inner button's action to the client.
- The composite is a `NamingContainer`; IDs inside are automatically scoped.
- Registered via `<composite-library-name>components</composite-library-name>` in `tags.taglib.xml`.

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
