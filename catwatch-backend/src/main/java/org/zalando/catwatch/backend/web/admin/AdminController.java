package org.zalando.catwatch.backend.web.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.zalando.catwatch.backend.model.util.Scorer;
import org.zalando.catwatch.backend.repo.ContributorRepository;
import org.zalando.catwatch.backend.repo.ProjectRepository;
import org.zalando.catwatch.backend.repo.StatisticsRepository;
import org.zalando.catwatch.backend.repo.util.DatabasePopulator;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Arrays.stream;
import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

@Controller
public class AdminController {

    private final ContributorRepository contributorRepository;
    private final StatisticsRepository statisticsRepository;
    private final ProjectRepository projectRepository;
    private final DatabasePopulator databasePopulator;
    private final Scorer scorer;
    private final String organizations;
    private final String scoringProject;

    @Autowired
    public AdminController(ContributorRepository contributorRepository,
                           StatisticsRepository statisticsRepository,
                           ProjectRepository projectRepository,
                           DatabasePopulator databasePopulator,
                           Scorer scorer,
                           @Value("${organization.list}") String organizations,
                           @Value("${scoring.project}") String scoringProject) {
        this.contributorRepository = contributorRepository;
        this.statisticsRepository = statisticsRepository;
        this.projectRepository = projectRepository;
        this.databasePopulator = databasePopulator;
        this.scorer = scorer;
        this.organizations = organizations;
        this.scoringProject = scoringProject;
    }

    @RequestMapping(value = "/config/scoring.project", method = POST, produces = "application/json; charset=utf-8")
    @ResponseBody
    public List<String> configScoringProjects(@RequestBody(required=false) String scoringProject,
                                              @RequestHeader(value="X-Organizations", required=false) String organizations) {

        // +++ initialize the parameters
        if (organizations == null) {
            organizations = this.organizations;
        }

        if (scoringProject == null) {
            scoringProject = this.scoringProject;
        }

        // +++ update the score function
        scorer.setScoringProject(scoringProject);

        // +++ update the scores for all projects of the latest snapshot
        List<String> messages = new ArrayList<>();
        final AtomicInteger processedProjects = new AtomicInteger();
        

        stream(organizations.trim().split("\\s*,\\s*")).forEach(organization -> {

            projectRepository.findProjects(organization, empty(), empty()).forEach(project -> {

                if (messages.size() > 5) {
                    return;
                }

                try {

                    project.setScore(scorer.score(project));
                    projectRepository.save(project);
                    processedProjects.incrementAndGet();

                } catch (Exception e) {

                    if (messages.size() == 0) {
                        e.printStackTrace();
                    }
                    messages.add("project " + project.getName() + ": " + e.getMessage());

                }
            });

        });

        if (messages.size() > 5) {
            messages.add("score update stopped due to errors");
        }
        return messages.size() == 0 ? singletonList(processedProjects + " project object(s) updated") : messages;
    }

    @RequestMapping(value = "/init", method = GET, produces = "application/json; charset=utf-8")
    @ResponseBody
    public String init() {
        databasePopulator.populateTestData();
        return "OK";
    }

    @RequestMapping(value = "/delete", method = GET, produces = "application/json; charset=utf-8")
    @ResponseBody
    public String deleteAll() {
        contributorRepository.deleteAll();
        projectRepository.deleteAll();
        statisticsRepository.deleteAll();
        return "OK";
    }

    @RequestMapping(value = "/import", method = POST, produces = "application/json; charset=utf-8")
    @ResponseBody
    public String importJson(@RequestBody DatabaseDto dto) {
        contributorRepository.save(dto.contributors);
        projectRepository.save(dto.projects); // erroneous as the ID of projects is generated by the database
        statisticsRepository.save(dto.statistics);
        return "OK";
    }

    @RequestMapping(value = "/export", method = GET, produces = "application/json; charset=utf-8")
    @ResponseBody
    public DatabaseDto exportJson() {
        DatabaseDto dto = new DatabaseDto();
        dto.contributors.addAll(newArrayList(contributorRepository.findAll()));
        dto.projects.addAll(newArrayList(projectRepository.findAll()));
        dto.statistics.addAll(newArrayList(statisticsRepository.findAll()));
        return dto;
    }
}
