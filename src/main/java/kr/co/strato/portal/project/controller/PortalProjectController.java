package kr.co.strato.portal.project.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import kr.co.strato.global.model.PageRequest;
import kr.co.strato.global.model.ResponseWrapper;
import kr.co.strato.portal.project.model.ProjectDto;
import kr.co.strato.portal.project.service.PortalProjectService;

@RestController
public class PortalProjectController {

	@Autowired
	PortalProjectService portalProjectService;
	
	/**
     * Project 리스트 조회
     * @param pageRequest
     * @return
     */
    @GetMapping("/api/v1/project/projects")
    @ResponseStatus(HttpStatus.OK)
    public ResponseWrapper<Page<ProjectDto>> getProjectList(@RequestParam int page, @RequestParam int size, @RequestBody ProjectDto param){
        
    	PageRequest pageable = new PageRequest();
    	pageable.setPage(page);
    	pageable.setSize(size);
    	
    	Page<ProjectDto> response = portalProjectService.getProjectList(pageable.of(), param);
    	//List<ProjectUserDto> response = portalProjectService.getProjectList(pageable.of(), dto.getUserId());
        
        return new ResponseWrapper<Page<ProjectDto>>(response);
    	//return new ResponseWrapper<List<ProjectUserDto>>(response);
    }
}