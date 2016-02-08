package net.spantree.meetupgenius.batch

import groovy.util.logging.Slf4j;
import net.spantree.meetupgenius.domain.MeetupEntity
import net.spantree.meetupgenius.domain.MeetupProperty
import org.neo4j.graphdb.Direction
import org.neo4j.ogm.annotation.NodeEntity
import org.neo4j.ogm.annotation.Relationship
import org.springframework.batch.item.ItemWriter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.annotation.AnnotationUtils
import org.springframework.dao.DataAccessException
import org.springframework.data.neo4j.template.Neo4jOperations

import java.lang.reflect.Field
import java.lang.reflect.Method;

@Slf4j
//@CompileStatic
public class MeetupEntityWriter implements ItemWriter<Map<String, Object>> {
    @Autowired
    Neo4jOperations neo4jTemplate

    public final static String RELATIONSHIP_MAP_KEY = "_relationships"

    private Class<? extends MeetupEntity> entityClass
    private String entityLabel = null

    private Map<String, String> meetupPropertyMap
    private Method idFinder
    String idField = null

    public void setEntityClass(final Class<? extends MeetupEntity> entityClass) {
        this.entityClass = entityClass
        entityLabel = AnnotationUtils.findAnnotation(entityClass, NodeEntity)?.label() ?: entityClass.getSimpleName()
        meetupPropertyMap = [:]
        entityClass.declaredFields.each { Field field ->
            MeetupProperty annotation = AnnotationUtils.getAnnotation(field, MeetupProperty)
            if(annotation) {
                def propName = annotation.name()
                meetupPropertyMap[propName] = field.name
                if(field.name == "meetupId") {
                    idField = annotation.name()
                }
            }
        }
    }

    public Relationship getRelationshipAnnotation(Class<? extends MeetupEntity> nodeClass, String fieldName) {
        Field field = nodeClass.declaredFields.find { it.name == fieldName }
        (Relationship)AnnotationUtils.getAnnotation(field, Relationship)
    }

    private String shortNodeEntityString(MeetupEntity e) {
        "${e.getClass().getSimpleName()}:${e.meetupId}"
    }

    private void addRelationshipsToEntity(MeetupEntity e1, newRelationshipMap) {
        for (String relationshipField : newRelationshipMap.keySet()) {
            MeetupEntity e2 = newRelationshipMap[relationshipField]
            def relationship = getRelationshipAnnotation(e1.getClass(), relationshipField)
            def relationshipFlow = relationship.direction() == Direction.OUTGOING.name() ?
                "-[r:${relationship.type()}]->"
                : "<-[r:${relationship.type()}]-"

            def queryParams = [
                n1Id: e1.nodeId,
                n2Id: e2.nodeId
            ]
            def results = neo4jTemplate.query("""
                MATCH (n1)${relationshipFlow}(n2)
                WHERE
                    ID(n1) = {n1Id}
                    AND ID(n2) = {n2Id}
                RETURN
                    count(r) AS count
            """, queryParams)
            def relationshipAlreadyExists = results.queryResults().first().get("count") != 0
            if (!relationshipAlreadyExists) {
                log.info "Adding relationship (${shortNodeEntityString(e1)})${relationshipFlow}(${shortNodeEntityString(e2)})"
                neo4jTemplate.query("""
                    MATCH n1, n2
                    WHERE
                        ID(n1) = {n1Id}
                        AND ID(n2) = {n2Id}
                    CREATE (n1)${relationshipFlow}(n2)
                """, queryParams)
            }
        }
    }

    @Override
    public void write(List<? extends Map<String, Object>> items) throws Exception {
        items.forEach({ Map item ->
            try {
                def meetupId = item[idField]
                def existing = neo4jTemplate.queryForObject(
                    entityClass,
                    "MATCH (n:${entityLabel} {meetupId: {meetupId}}) RETURN n",
                    [meetupId: meetupId]
                )
                MeetupEntity e = existing ?: entityClass.newInstance()
                Map<String, Object> setValues = [:]
                for (String meetupProperty : meetupPropertyMap.keySet()) {
                    def graphProperty = meetupPropertyMap[meetupProperty]
                    def oldValue = e.getProperty(graphProperty)
                    def newValue = item[meetupProperty]
                    if (newValue != null && oldValue != newValue) {
                        setValues[graphProperty] = newValue
                    }
                }
                if (setValues.size() > 0) {
                    setValues.meetupId = meetupId

                    def setClause = setValues
                        .findAll { k, v -> k != "meetupId" }
                        .collect { k, v -> "n.${k} = {${k}}" }.join(", ")

                    def updateQuery = """
                        MERGE (n:${entityLabel} { meetupId: {meetupId} })
                        SET ${setClause}
                        RETURN n
                    """.toString()

                    log.info "Writing ${entityClass.simpleName}: ${setValues}"
                    e = neo4jTemplate.queryForObject(entityClass, updateQuery, setValues)
                } else {
                    log.info "Skipping ${e} due to no update"
                }
                if (item.containsKey(RELATIONSHIP_MAP_KEY)) {
                    def newRelationshipMap = item._relationships
                    addRelationshipsToEntity(e, newRelationshipMap)
                }
            } catch (DataAccessException e) {
                log.error "Error accessing data", e
            }
        })
    }
}
