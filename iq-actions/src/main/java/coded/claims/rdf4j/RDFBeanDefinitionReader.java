package systems.symbol.rdf4j;

import systems.symbol.bean.XSD2POJOConverter;
import systems.symbol.ns.COMMONS;
import systems.symbol.rdf4j.util.RDFCollections;
import systems.symbol.rdf4j.util.RDFScalars;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinitionReader;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * systems.symbol (c) 2014-2023
 * Module: systems.symbol.vendor.spring
 * @author Symbol Systems
 * Date  : 18/06/2014
 * Time  : 9:27 AM
 */
public class RDFBeanDefinitionReader extends AbstractBeanDefinitionReader implements COMMONS {
    static protected final Logger log = LoggerFactory.getLogger(RDFBeanDefinitionReader.class);

    public static String PROTOCOL = "bean:";
    String NS = "urn:bean:";
    String BEAN = NS +"Bean";
    String ontology = NS;
    boolean useInferencing = true;
    RepositoryConnection connection;
    ValueFactory vf = null;
    XSD2POJOConverter converter = new XSD2POJOConverter();
    protected Map reserved = new HashMap();
    RDFCollections rdfCollections;
    RDFScalars rdfScalars;


    public RDFBeanDefinitionReader(RepositoryConnection connection) {
        this(connection, new GenericApplicationContext());
    }

    public RDFBeanDefinitionReader(RepositoryConnection connection, BeanDefinitionRegistry registry) {
        super(registry);
        Assert.notNull(connection, "RepositoryConnection can't be NULL");
        this.connection=connection;
        vf = connection.getValueFactory();
        rdfCollections = new RDFCollections(connection);
        rdfScalars = new RDFScalars(connection);
        setBeanClassLoader(Thread.currentThread().getContextClassLoader());
        defaultReservedWords();
    }

    public void defaultReservedWords() {
        reserved.put("new", Collection.class);
        reserved.put("dependsOn", Collection.class);
        reserved.put("constructor", Collection.class);
    }


    public void alias(String name, String alias) {
        getRegistry().registerAlias(name, alias);
    }


    @Override
    public int loadBeanDefinitions(String resource) throws BeanDefinitionStoreException {
        try {
            return read(resource);
        } catch (RepositoryException e) {
            throw new BeanDefinitionStoreException("Bean Repository Error: "+e.getMessage(),e);
        } catch (ClassNotFoundException e) {
            throw new BeanDefinitionStoreException("Bean Class Not Found: "+e.getMessage(),e);
        } catch (IOException e) {
            throw new BeanDefinitionStoreException("Bean Class I/O Error: "+e.getMessage(),e);
        }
    }

    @Override
    public int loadBeanDefinitions(Resource resource) throws BeanDefinitionStoreException {
        try {
            return read(resource);
        } catch (RepositoryException e) {
            throw new BeanDefinitionStoreException("Repository Error: "+e.getMessage(),e);
        } catch (IOException e) {
            throw new BeanDefinitionStoreException("IO Error: "+e.getMessage(),e);
        } catch (ClassNotFoundException e) {
            throw new BeanDefinitionStoreException("Class Not Found: "+e.getMessage(),e);
        }
    }

    public int read(Resource resource) throws RepositoryException, IOException, ClassNotFoundException {
        Assert.notNull(resource, "Resource must not be NULL");
        String iri = resource.getURI().toString();
        return read(vf.createIRI(iri));
    }

    public int read(String resource) throws RepositoryException, IOException, ClassNotFoundException {
        Assert.notNull(resource, "Resource must not be NULL");
        return read(vf.createIRI(resource));
    }

    private int read(IRI resourceURI) throws RepositoryException, ClassNotFoundException {
        int count = 0;

        if (resourceURI.stringValue().startsWith(PROTOCOL)) {
            count+= readPrototype(resourceURI);
        } else {
            count+= readSingleton(resourceURI);
        }
        return count;

    }

    private int readSingleton(IRI resourceURI) throws RepositoryException, ClassNotFoundException {
        int count = 0;
        RepositoryResult<Statement> beans = connection.getStatements(resourceURI, RDF.TYPE, null, useInferencing);
        while(beans.hasNext()) {
            Statement bean = beans.next();
            boolean isBean = bean.getObject().stringValue().startsWith(PROTOCOL);
            boolean knownBean = getRegistry().containsBeanDefinition(bean.getSubject().stringValue());
            if (!knownBean && isBean && bean.getObject() instanceof org.eclipse.rdf4j.model.Resource) {
                org.eclipse.rdf4j.model.Resource classIRI = (org.eclipse.rdf4j.model.Resource) bean.getObject();
                count+= readBean(bean.getSubject(), classIRI, BeanDefinition.SCOPE_SINGLETON);
//			} else {
//				log.warn("Can't Register Singleton: "+resourceURI);
            }
        }
        if (count>0) log.debug("Read Bean Singleton: "+resourceURI);
        else log.debug("UNKNOWN Bean Singleton: "+resourceURI);

        return count;
    }

    private int readPrototype(org.eclipse.rdf4j.model.Resource resourceURI) throws RepositoryException, ClassNotFoundException {
        return readBean(resourceURI, resourceURI, BeanDefinition.SCOPE_PROTOTYPE);
    }

    private int readBean(org.eclipse.rdf4j.model.Resource resource, org.eclipse.rdf4j.model.Resource classIRI, String scope) throws RepositoryException, ClassNotFoundException {
        int count = 0;
        log.debug("readBean("+scope+") -> "+ resource+" @ "+classIRI);
        RepositoryResult<Statement> beans = connection.getStatements(classIRI, RDF.TYPE, vf.createIRI(BEAN), useInferencing);
        while(beans.hasNext()) {
            Statement bean = beans.next();
            String beanClass = bean.getSubject().stringValue();
            if (!getRegistry().containsBeanDefinition(resource.stringValue())) {
                AbstractBeanDefinition beanDefinition = defineBean(resource, bean);
                beanDefinition.setScope(scope);
                if (beanDefinition!=null) {
                    getRegistry().registerBeanDefinition(resource.stringValue(), beanDefinition);
                    log.debug(count+") Registered: "+resource+" @ "+beanClass+" -> "+beanDefinition);
                    count++;
                }
            } else {
//s				log.trace("Re-Register: " + beanClass);
            }
        }
        return count;
    }

    private AbstractBeanDefinition defineBean(org.eclipse.rdf4j.model.Resource beanURI, Statement bean) throws RepositoryException, ClassNotFoundException {
        String beanClass = bean.getSubject().stringValue();
        AbstractBeanDefinition defineBean = defineBean(beanClass);

        /*
         * Scalar Definitions
         */

        Literal lazyInit = rdfScalars.getLiteral(beanURI, createIRI( "lazyInit"), BOOLEAN);
        if (lazyInit!=null) defineBean.setLazyInit(lazyInit.booleanValue());

        Literal initMethod = rdfScalars.getLiteral(beanURI, createIRI( "initMethod"), STRING);
        if (initMethod!=null) defineBean.setInitMethodName(initMethod.stringValue());

        Literal destroyMethod = rdfScalars.getLiteral(beanURI, createIRI( "destroyMethod"), STRING);
        if (destroyMethod!=null) defineBean.setDestroyMethodName(destroyMethod.stringValue());

        Literal lenient = rdfScalars.getLiteral(beanURI, createIRI( "lenient"), BOOLEAN);
        if (lenient!=null) defineBean.setLenientConstructorResolution(lenient.booleanValue());

        Literal singleton = rdfScalars.getLiteral(beanURI, createIRI( "singleton"), BOOLEAN);
        boolean isSingleton = connection.hasStatement(bean.getSubject(), RDF.TYPE, bean.getSubject(), false);
        if ( (singleton!=null && singleton.booleanValue()) || isSingleton) {
            defineBean.setScope(BeanDefinition.SCOPE_SINGLETON);
        }

        Literal enforceInit = rdfScalars.getLiteral(beanURI, createIRI( "enforceInit"), BOOLEAN);
        if (enforceInit!=null) defineBean.setEnforceInitMethod(enforceInit.booleanValue());

        Literal enforceDestroy = rdfScalars.getLiteral(beanURI, createIRI( "enforceDestroy"), BOOLEAN);
        if (enforceDestroy!=null) defineBean.setEnforceDestroyMethod(enforceDestroy.booleanValue());

        Literal primary = rdfScalars.getLiteral(beanURI, createIRI( "primary"), BOOLEAN);
        if (primary!=null) defineBean.setPrimary(primary.booleanValue());

        Literal autoWire = rdfScalars.getLiteral(beanURI, createIRI( "autoWire"), BOOLEAN);
        if (autoWire!=null) defineBean.setAutowireCandidate(autoWire.booleanValue());

        Literal description = rdfScalars.getLiteral(beanURI, createIRI( "description"), STRING);
        if (description!=null) defineBean.setDescription(description.stringValue());

        /*
         * Vector Definitions
         */

        // constructor arguments
        ConstructorArgumentValues argValues = new ConstructorArgumentValues();
        int i = createConstructor(0, beanURI, argValues, createIRI( "new"));
        if (i==0) i = createConstructor(0, beanURI, argValues, createIRI( "constructor"));
        defineBean.setConstructorArgumentValues(argValues);

        // dependsOn
        Collection<Value> dependsList = rdfCollections.getList(beanURI, createIRI( "dependsOn"));
        String[] dependsOn = new String[dependsList.size()];
        i=0;
        for(Value depends: dependsList) {
            dependsOn[i++]=depends.stringValue();
        }
        if (i>0) {
            log.debug("\tDepends On: "+dependsOn);
            defineBean.setDependsOn(dependsOn);
        }

        MutablePropertyValues propertyValues = defineBeanProperties(beanURI, defineBean);
        log.debug("Defined: "+beanClass+" @ "+defineBean);
        log.debug("\tproperties:"+Arrays.toString(propertyValues.getPropertyValues()));
        return defineBean;
    }

    private int createConstructor(int i, org.eclipse.rdf4j.model.Resource beanURI, ConstructorArgumentValues argValues, IRI newPredicate) throws RepositoryException {
        Collection<Value> initArgs = this.rdfCollections.getList(beanURI, newPredicate);
        for(Value initValue: initArgs) {
            i = addToArguments(argValues, i, initValue);
        }
        if (i==0) {
            // handle single scalar reference
            Value arg = rdfScalars.getValue(beanURI, newPredicate);
            if (arg!=null) addToArguments(argValues, 0, arg);
        }
        return i;
    }

    protected MutablePropertyValues defineBeanProperties(org.eclipse.rdf4j.model.Resource beanURI, AbstractBeanDefinition defineBean) throws RepositoryException {
        // properties
        MutablePropertyValues propertyValues = defineBean.getPropertyValues();
        RepositoryResult<Statement> statements = connection.getStatements(beanURI, null, null, useInferencing);
        while(statements.hasNext()) {
            Statement next = statements.next();
            IRI iri = next.getPredicate();
            if (iri.toString().startsWith(getIdentity())) {
                String localName = iri.getLocalName();
                boolean isReserved = reserved.containsKey(localName);
                log.debug("\tProperty: "+localName+" = "+next.getObject()+(isReserved?" RESERVED":""));
                if (!isReserved) {
                    addToProperties(propertyValues, localName, next.getObject());
                }
            }
        }
        return propertyValues;
    }

    private IRI createIRI(String localPart) {
        return vf.createIRI(getBaseURI() + localPart);
    }

    private int addToArguments(ConstructorArgumentValues argValues, int ix, Value value) {
        if (value instanceof IRI) {
            String iri = value.stringValue();
            log.debug("\tRef: "+iri);
            argValues.addIndexedArgumentValue(ix++, new RuntimeBeanReference(iri));
        } else if (value instanceof Literal) {
            Literal literal = (Literal)value;
            IRI datatype = literal.getDatatype();
            if (datatype==null) datatype = vf.createIRI(STRING);
            log.debug("\tNew: "+literal+" @ "+datatype);

            Object o = converter.convertToType(literal.stringValue(), datatype.toString());
            argValues.addIndexedArgumentValue(ix++, o, o.getClass().getCanonicalName());
        } else {
            log.debug("\tinit? "+value);
        }
        return ix;
    }

    private void addToProperties(MutablePropertyValues propertyValues, String local, Value value) {
        if (value instanceof IRI) {
            String iri = value.stringValue();
            log.debug("\tNew Ref: "+iri);
            propertyValues.add(local, new RuntimeBeanReference(iri));
        } else if (value instanceof Literal) {
            Literal literal = (Literal) value;
            IRI datatype = literal.getDatatype();
            Object o = converter.convertToType(literal.stringValue(), datatype == null ? null : datatype.toString());
            log.debug("\t" + local + " == " + literal + " -> " + o + " = " + o.getClass() + " @ " + datatype);
            PropertyValue propertyValue = new PropertyValue(local, o);
            propertyValues.addPropertyValue(propertyValue);
        } else {
            log.debug("\tinit? " + value);
        }
    }

    protected AbstractBeanDefinition defineBean(String beanClass) throws ClassNotFoundException {
        if (beanClass.startsWith(PROTOCOL)) beanClass = beanClass.substring(5);
        GenericBeanDefinition beanDef = new GenericBeanDefinition();
        beanDef.setBeanClassName(beanClass);
        beanDef.setNonPublicAccessAllowed(true);
        beanDef.setPrimary(true);
        beanDef.setPropertyValues(new MutablePropertyValues());
        beanDef.setScope(GenericBeanDefinition.SCOPE_PROTOTYPE);
        beanDef.setLazyInit(true);
        beanDef.setSynthetic(false);
        beanDef.setAutowireMode(GenericBeanDefinition.AUTOWIRE_BY_NAME);
        beanDef.setAutowireCandidate(false);
        beanDef.setAbstract(false);
        beanDef.setLenientConstructorResolution(true);
        beanDef.setDependencyCheck(GenericBeanDefinition.DEPENDENCY_CHECK_NONE);
        assert beanDef.getDependencyCheck()==0;
        log.debug("Resolving: "+beanClass+" -> "+beanDef);
        beanDef.resolveBeanClass(getBeanClassLoader());
        return beanDef;
    }


    public String getBaseURI() {
        return NS;
    }

    public String getIdentity() {
        return ontology;
    }

    public Object getBean(String name) {
        BeanDefinitionRegistry registry = getRegistry();
        if (registry instanceof AbstractApplicationContext) {
            log.debug("getBean: "+name+" from "+((AbstractApplicationContext) registry).getClassLoader());
            AbstractApplicationContext applicationContext = (AbstractApplicationContext)registry;
            log.debug("Beans: " + Arrays.toString(applicationContext.getBeanDefinitionNames()));
            Object bean = applicationContext.getBean(name);
            return bean;
        }
        return null;
    }
}
