package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.model;

import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;


@Document(collection = "status")
public record NotificationStatus(
        @Id String id,
        @Field("requestId") String requestId,
        @Field("responseId") String responseId,
        @Field("status") String status,
        @Field("statusDetails") Map<String, Object> statusDetails // todo
)  {}
