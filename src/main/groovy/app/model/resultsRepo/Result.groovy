package app.model.resultsRepo

class Result {

    Date created_at
    Date ended_at
    List<Map<String, Object>> details
    List<Map<String, Object>> results
    String instance_uuid
    String package_id
    String service_uuid
    String status
    String stderr
    String stdout
    String test_plan_id
    String test_uuid
    Date updated_at
    String uuid
}
