package cz.muni.fi.pa165.ddtroops.mvc.controllers;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import cz.muni.fi.pa165.ddtroops.dto.UserCreateDTO;
import cz.muni.fi.pa165.ddtroops.dto.UserDTO;
import cz.muni.fi.pa165.ddtroops.dto.UserUpdateDTO;
import cz.muni.fi.pa165.ddtroops.facade.UserFacade;
import cz.muni.fi.pa165.ddtroops.mvc.Tools;
import cz.muni.fi.pa165.ddtroops.mvc.validators.UserCreateDTOValidator;
import cz.muni.fi.pa165.ddtroops.mvc.validators.UserUpdateDTOValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Created by pstanko.
 * @author pstanko
 */
@Controller
@RequestMapping("/users")
public class UsersController {

    private final static Logger log = LoggerFactory.getLogger(UsersController.class);

    @Autowired
    private UserFacade userFacade;

    @InitBinder
    protected void initBinder(WebDataBinder binder) {
        if (binder.getTarget() instanceof UserCreateDTO) {
            binder.addValidators(new UserCreateDTOValidator());
        }

        if (binder.getTarget() instanceof UserUpdateDTO) {
            binder.addValidators(new UserUpdateDTOValidator());
        }
    }

    @RequestMapping(value="", method = RequestMethod.GET)
    public String list(Model model, HttpServletRequest request, UriComponentsBuilder uriBuilder, RedirectAttributes redirectAttributes) {
        log.debug("[USERS] List all");
        String res = Tools.redirectNonAdmin(request, uriBuilder, redirectAttributes);
        if(res != null) return res;
        model.addAttribute("users", userFacade.findAll());
        return "users/list";
    }

    @RequestMapping(value = "/read/{id}", method = RequestMethod.GET)
    public String read(@PathVariable long id, Model model, UriComponentsBuilder uriBuilder, HttpServletRequest request) {
        UserDTO user = (UserDTO) request.getSession().getAttribute("user");

        if(!user.isAdmin() && user.getId() != id){
            return "redirect:" + uriBuilder.path("/").build().toUriString();
        }

        log.debug("[USERS] Read ({})", id);

        model.addAttribute("user", userFacade.findById(id));
        return "users/read";
    }

    @RequestMapping(value = "/delete/{id}", method = RequestMethod.POST)
    public String delete(@PathVariable long id, Model model, HttpServletRequest request, UriComponentsBuilder uriBuilder, RedirectAttributes redirectAttributes) {
        String res = Tools.redirectNonAdmin(request, uriBuilder, redirectAttributes);
        if(res != null) return res;

        UserDTO user = userFacade.findById(id);
        userFacade.delete(id);
        log.debug("delete user({})", id);
        redirectAttributes.addFlashAttribute("alert_success", "User \"" + user.getName() + "\" was deleted.");
        return "redirect:" + uriBuilder.path("/users").build().toUriString();
    }

    @RequestMapping(value = "/edit/{id}", method = RequestMethod.GET)
    public String editUser(@PathVariable long id, Model model, HttpServletRequest request, UriComponentsBuilder uriBuilder, RedirectAttributes redirectAttributes) {

        UserDTO logUser = (UserDTO) request.getSession().getAttribute("user");

        if(!logUser.isAdmin() && logUser.getId() != id){
            return "redirect:" + uriBuilder.path("/").build().toUriString();
        }

        log.debug("[USER] Edit {}", id);
        UserDTO userDTO = userFacade.findById(id);

        model.addAttribute("userEdit", userDTO);
        return "/users/edit";
    }

    @RequestMapping(value="/edit/{id}", method = RequestMethod.POST)
    public String update(@PathVariable long id,
        @Valid @ModelAttribute("userEdit")UserUpdateDTO formBean,
        BindingResult bindingResult,
        Model model,
        UriComponentsBuilder uriBuilder,
        RedirectAttributes redirectAttributes,
        HttpServletRequest request) {

        UserDTO logUser = (UserDTO) request.getSession().getAttribute("user");

        if(!logUser.isAdmin() && logUser.getId() != id){
            return "redirect:" + uriBuilder.path("/").build().toUriString();
        }

        formBean.setId(id);

        if(!logUser.isAdmin() && logUser.getId() == id){
            formBean.setAdmin(false);
        }

        log.debug("User - update");
        if (bindingResult.hasErrors()) {
            log.debug("User - has errors:");

            for (ObjectError ge : bindingResult.getGlobalErrors()) {
                log.debug("ObjectError: {}", ge);
            }
            for (FieldError fe : bindingResult.getFieldErrors()) {
                model.addAttribute(fe.getField() + "_error", true);
                log.debug("FieldError: {}", fe);
            }
            model.addAttribute("userEdit", formBean);
            return "users/edit";
        }

        log.debug("[USER] Update: {}", formBean);
        UserDTO result = userFacade.update(formBean);

        redirectAttributes.addFlashAttribute("alert_success", "User " + result.getEmail() + " was updated");
        return "redirect:" + uriBuilder.path("/users/read/{id}").buildAndExpand(id).encode().toUriString();
    }

}
