package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.healthcheck;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Returns HTTP OK response to indicate a healthy service is running
 */
@RestController
public class HealthcheckController {
    @GetMapping("${uk.gov.companieshouse.orders.api.health}")
    public ResponseEntity<Void> getHealthCheck (){
        return ResponseEntity.status(HttpStatus.OK).build();
    }
}
