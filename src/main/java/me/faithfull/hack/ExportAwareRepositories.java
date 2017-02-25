package me.faithfull.hack;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.repository.core.EntityInformation;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.support.RepositoryFactoryInformation;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import java.io.Serializable;
import java.util.*;

/**
 * @author Will Faithfull
 *
 * Warning: Ugly hack territory.
 *
 * Firstly, I can't just swap out this implementation, because Repositories is referenced everywhere directly without an
 * interface.
 *
 * Unfortunately, the offending code is in a private method, {@link #cacheRepositoryFactory(String)}, and modifies private
 * fields in the Repositories class. This means we can either use reflection, or replicate the functionality of the class.
 *
 * In this instance, I've chosen to do the latter because it's simpler, and most of this code is a simple copy/paste from
 * Repositories. The superclass is given an empty bean factory to satisfy it's constructor demands, and ensure that
 * it will keep as little redundant state as possible.
 */
public class ExportAwareRepositories extends Repositories {

    static final Repositories NONE = new ExportAwareRepositories();

    private static final RepositoryFactoryInformation<Object, Serializable> EMPTY_REPOSITORY_FACTORY_INFO = EmptyRepositoryFactoryInformation.INSTANCE;
    private static final String DOMAIN_TYPE_MUST_NOT_BE_NULL = "Domain type must not be null!";

    private final BeanFactory beanFactory;
    private final Map<Class<?>, String> repositoryBeanNames;
    private final Map<Class<?>, RepositoryFactoryInformation<Object, Serializable>> repositoryFactoryInfos;

    /**
     * Constructor to create the {@link #NONE} instance.
     */
    private ExportAwareRepositories() {
        /* Mug off the superclass with an empty beanfactory to placate the Assert.notNull */
        super(new DefaultListableBeanFactory());
        this.beanFactory = null;
        this.repositoryBeanNames = Collections.<Class<?>, String> emptyMap();
        this.repositoryFactoryInfos = Collections.<Class<?>, RepositoryFactoryInformation<Object, Serializable>> emptyMap();
    }

    /**
     * Creates a new {@link Repositories} instance by looking up the repository instances and meta information from the
     * given {@link ListableBeanFactory}.
     *
     * @param factory must not be {@literal null}.
     */
    public ExportAwareRepositories(ListableBeanFactory factory) {
        /* Mug off the superclass with an empty beanfactory to placate the Assert.notNull */
        super(new DefaultListableBeanFactory());
        Assert.notNull(factory, "Factory must not be null!");

        this.beanFactory = factory;
        this.repositoryFactoryInfos = new HashMap<Class<?>, RepositoryFactoryInformation<Object, Serializable>>();
        this.repositoryBeanNames = new HashMap<Class<?>, String>();

        populateRepositoryFactoryInformation(factory);
    }

    private void populateRepositoryFactoryInformation(ListableBeanFactory factory) {

        for (String name : BeanFactoryUtils.beanNamesForTypeIncludingAncestors(factory, RepositoryFactoryInformation.class,
                false, false)) {
            cacheRepositoryFactory(name);
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private synchronized void cacheRepositoryFactory(String name) {

        RepositoryFactoryInformation repositoryFactoryInformation = beanFactory.getBean(name,
                RepositoryFactoryInformation.class);
        Class<?> domainType = ClassUtils
                .getUserClass(repositoryFactoryInformation.getRepositoryInformation().getDomainType());

        RepositoryInformation information = repositoryFactoryInformation.getRepositoryInformation();
        Set<Class<?>> alternativeDomainTypes = information.getAlternativeDomainTypes();
        String beanName = BeanFactoryUtils.transformedBeanName(name);

        Set<Class<?>> typesToRegister = new HashSet<Class<?>>(alternativeDomainTypes.size() + 1);
        typesToRegister.add(domainType);
        typesToRegister.addAll(alternativeDomainTypes);

        for (Class<?> type : typesToRegister) {
            // I still want to add repositories if they don't have an exported counterpart, so we eagerly add repositories
            // but then check whether to supercede them. If you have more than one repository with exported=true, clearly
            // the last one that arrives here will be the registered one. I don't know why anyone would do this though.
            if(this.repositoryFactoryInfos.containsKey(type)) {
                Class<?> repoInterface = information.getRepositoryInterface();
                if(repoInterface.isAnnotationPresent(RepositoryRestResource.class)) {
                    boolean exported = repoInterface.getAnnotation(RepositoryRestResource.class).exported();

                    if(exported) { // Then this has priority.
                        this.repositoryFactoryInfos.put(type, repositoryFactoryInformation);
                        this.repositoryBeanNames.put(type, beanName);
                    }
                }
            } else {
                this.repositoryFactoryInfos.put(type, repositoryFactoryInformation);
                this.repositoryBeanNames.put(type, beanName);
            }
        }
    }

    /**
     * Returns whether we have a repository instance registered to manage instances of the given domain class.
     *
     * @param domainClass must not be {@literal null}.
     * @return
     */
    @Override
    public boolean hasRepositoryFor(Class<?> domainClass) {

        Assert.notNull(domainClass, DOMAIN_TYPE_MUST_NOT_BE_NULL);

        return repositoryFactoryInfos.containsKey(domainClass);
    }

    /**
     * Returns the repository managing the given domain class.
     *
     * @param domainClass must not be {@literal null}.
     * @return
     */
    @Override
    public Object getRepositoryFor(Class<?> domainClass) {

        Assert.notNull(domainClass, DOMAIN_TYPE_MUST_NOT_BE_NULL);

        String repositoryBeanName = repositoryBeanNames.get(domainClass);
        return repositoryBeanName == null || beanFactory == null ? null : beanFactory.getBean(repositoryBeanName);
    }

    /**
     * Returns the {@link RepositoryFactoryInformation} for the given domain class. The given <code>code</code> is
     * converted to the actual user class if necessary, @see ClassUtils#getUserClass.
     *
     * @param domainClass must not be {@literal null}.
     * @return the {@link RepositoryFactoryInformation} for the given domain class or {@literal null} if no repository
     *         registered for this domain class.
     */
    private RepositoryFactoryInformation<Object, Serializable> getRepositoryFactoryInfoFor(Class<?> domainClass) {

        Assert.notNull(domainClass, DOMAIN_TYPE_MUST_NOT_BE_NULL);

        Class<?> userType = ClassUtils.getUserClass(domainClass);
        RepositoryFactoryInformation<Object, Serializable> repositoryInfo = repositoryFactoryInfos.get(userType);

        if (repositoryInfo != null) {
            return repositoryInfo;
        }

        if (!userType.equals(Object.class)) {
            return getRepositoryFactoryInfoFor(userType.getSuperclass());
        }

        return EMPTY_REPOSITORY_FACTORY_INFO;
    }

    /**
     * Returns the {@link EntityInformation} for the given domain class.
     *
     * @param domainClass must not be {@literal null}.
     * @return
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T, S extends Serializable> EntityInformation<T, S> getEntityInformationFor(Class<?> domainClass) {

        Assert.notNull(domainClass, DOMAIN_TYPE_MUST_NOT_BE_NULL);

        return (EntityInformation<T, S>) getRepositoryFactoryInfoFor(domainClass).getEntityInformation();
    }

    /**
     * Returns the {@link RepositoryInformation} for the given domain class.
     *
     * @param domainClass must not be {@literal null}.
     * @return the {@link RepositoryInformation} for the given domain class or {@literal null} if no repository registered
     *         for this domain class.
     */
    @Override
    public RepositoryInformation getRepositoryInformationFor(Class<?> domainClass) {

        Assert.notNull(domainClass, DOMAIN_TYPE_MUST_NOT_BE_NULL);

        RepositoryFactoryInformation<Object, Serializable> information = getRepositoryFactoryInfoFor(domainClass);
        return information == EMPTY_REPOSITORY_FACTORY_INFO ? null : information.getRepositoryInformation();
    }

    /**
     * Returns the {@link RepositoryInformation} for the given repository interface.
     *
     * @param repositoryInterface must not be {@literal null}.
     * @return the {@link RepositoryInformation} for the given repository interface or {@literal null} there's no
     *         repository instance registered for the given interface.
     * @since 1.12
     */
    @Override
    public RepositoryInformation getRepositoryInformation(Class<?> repositoryInterface) {

        for (RepositoryFactoryInformation<Object, Serializable> factoryInformation : repositoryFactoryInfos.values()) {

            RepositoryInformation information = factoryInformation.getRepositoryInformation();

            if (information.getRepositoryInterface().equals(repositoryInterface)) {
                return information;
            }
        }

        return null;
    }

    /**
     * Returns the {@link PersistentEntity} for the given domain class. Might return {@literal null} in case the module
     * storing the given domain class does not support the mapping subsystem.
     *
     * @param domainClass must not be {@literal null}.
     * @return the {@link PersistentEntity} for the given domain class or {@literal null} if no repository is registered
     *         for the domain class or the repository is not backed by a {@link MappingContext} implementation.
     */
    @Override
    public PersistentEntity<?, ?> getPersistentEntity(Class<?> domainClass) {

        Assert.notNull(domainClass, DOMAIN_TYPE_MUST_NOT_BE_NULL);
        return getRepositoryFactoryInfoFor(domainClass).getPersistentEntity();
    }

    /**
     * Returns the {@link QueryMethod}s contained in the repository managing the given domain class.
     *
     * @param domainClass must not be {@literal null}.
     * @return
     */
    @Override
    public List<QueryMethod> getQueryMethodsFor(Class<?> domainClass) {

        Assert.notNull(domainClass, DOMAIN_TYPE_MUST_NOT_BE_NULL);
        return getRepositoryFactoryInfoFor(domainClass).getQueryMethods();
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Iterable#iterator()
     */
    @Override
    public Iterator<Class<?>> iterator() {
        return repositoryFactoryInfos.keySet().iterator();
    }

    /**
     * Null-object to avoid nasty {@literal null} checks in cache lookups.
     *
     * @author Thomas Darimont
     */
    private static enum EmptyRepositoryFactoryInformation implements RepositoryFactoryInformation<Object, Serializable> {

        INSTANCE;

        @Override
        public EntityInformation<Object, Serializable> getEntityInformation() {
            return null;
        }

        @Override
        public RepositoryInformation getRepositoryInformation() {
            return null;
        }

        @Override
        public PersistentEntity<?, ?> getPersistentEntity() {
            return null;
        }

        @Override
        public List<QueryMethod> getQueryMethods() {
            return Collections.<QueryMethod> emptyList();
        }
    }
}