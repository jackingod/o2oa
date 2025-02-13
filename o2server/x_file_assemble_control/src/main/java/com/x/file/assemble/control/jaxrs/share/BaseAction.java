package com.x.file.assemble.control.jaxrs.share;

import com.x.base.core.project.http.EffectivePerson;
import com.x.base.core.project.jaxrs.StandardJaxrsAction;
import com.x.base.core.project.organization.Unit;
import com.x.file.assemble.control.Business;
import com.x.file.assemble.control.service.FileCommonService;
import com.x.file.core.entity.personal.Share;
import com.x.file.core.entity.personal.Share_;
import org.apache.commons.lang3.StringUtils;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.List;

abstract class BaseAction extends StandardJaxrsAction {

    protected FileCommonService fileCommonService = new FileCommonService();

    protected boolean exist(Business business, EffectivePerson effectivePerson, String name,
                            String excludeId) throws Exception {
        EntityManager em = business.entityManagerContainer().get(Share.class);
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<Share> root = cq.from(Share.class);
        Predicate p = cb.equal(root.get(Share_.person), effectivePerson.getDistinguishedName());
        p = cb.and(p, cb.equal(root.get(Share_.fileId), name));
        if (StringUtils.isNotEmpty(excludeId)) {
            p = cb.and(p, cb.notEqual(root.get(Share_.id), excludeId));
        }
        cq.select(cb.count(root)).where(p);
        long count = em.createQuery(cq).getSingleResult();
        return count > 0;
    }

    protected boolean hasPermission(Business business, EffectivePerson effectivePerson, Share share) throws Exception {
        boolean flag = true;
        if (!StringUtils.equals(effectivePerson.getDistinguishedName(), share.getPerson())
                && (!share.getShareUserList().contains(effectivePerson.getDistinguishedName()))) {
            flag = false;
            List<String> identities = business.organization().identity().listWithPerson(effectivePerson);
            for (String str : identities) {
                List<String> units = business.organization().unit().listWithIdentitySupNested(str);
                for (String unitName : units) {
                    Unit unit = business.organization().unit().getObject(unitName);
                    if (unit != null) {
                        if (share.getShareOrgList().contains(unit.getUnique())) {
                            flag = true;
                            break;
                        }
                    }
                }
                if (flag) {
                    break;
                }
            }
        }
        return flag;
    }

}
