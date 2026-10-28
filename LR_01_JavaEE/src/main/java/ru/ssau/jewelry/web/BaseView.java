package ru.ssau.jewelry.web;

import jakarta.ejb.EJBException;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.component.UIComponent;
import jakarta.faces.component.UIInput;
import jakarta.faces.component.visit.VisitContext;
import jakarta.faces.component.visit.VisitResult;
import java.io.Serializable;
import ru.ssau.jewelry.service.BusinessException;

public abstract class BaseView implements Serializable {
    private static final long serialVersionUID = 1L;

    protected boolean execute(Runnable action, String successMessage) {
        try {
            action.run();
            message(FacesMessage.SEVERITY_INFO, successMessage);
            return true;
        } catch (BusinessException exception) {
            message(FacesMessage.SEVERITY_ERROR, exception.getMessage());
        } catch (EJBException exception) {
            message(FacesMessage.SEVERITY_ERROR,
                    "Не удалось сохранить изменения. Обновите страницу и повторите действие. "
                            + "При повторении ошибки проверьте доступность базы данных и уникальность данных");
        }
        return false;
    }

    private void message(FacesMessage.Severity severity, String text) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(severity, text, null));
    }

    protected void resetEditor() {
        FacesContext context = FacesContext.getCurrentInstance();
        UIComponent editor = context.getViewRoot().findComponent("editor");
        editor.visitTree(VisitContext.createVisitContext(context), (visitContext, component) -> {
            if (component instanceof UIInput input) {
                input.resetValue();
            }
            return VisitResult.ACCEPT;
        });
    }
}
