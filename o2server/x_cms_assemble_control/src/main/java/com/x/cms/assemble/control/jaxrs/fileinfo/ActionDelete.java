package com.x.cms.assemble.control.jaxrs.fileinfo;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import com.x.base.core.container.EntityManagerContainer;
import com.x.base.core.container.factory.EntityManagerContainerFactory;
import com.x.base.core.entity.annotation.CheckRemoveType;
import com.x.base.core.project.cache.ApplicationCache;
import com.x.base.core.project.config.StorageMapping;
import com.x.base.core.project.http.ActionResult;
import com.x.base.core.project.http.EffectivePerson;
import com.x.base.core.project.jaxrs.WoId;
import com.x.base.core.project.logger.Logger;
import com.x.base.core.project.logger.LoggerFactory;
import com.x.cms.assemble.control.Business;
import com.x.cms.assemble.control.ThisApplication;
import com.x.cms.core.entity.Document;
import com.x.cms.core.entity.FileInfo;

public class ActionDelete extends BaseAction {

	private static  Logger logger = LoggerFactory.getLogger(ActionDelete.class);

	protected ActionResult<Wo> execute( HttpServletRequest request, EffectivePerson effectivePerson, String id ) throws Exception {
		ActionResult<Wo> result = new ActionResult<>();
		FileInfo fileInfo = null;
		Document document = null;
		
		Boolean isAnonymous = effectivePerson.isAnonymous();
		Boolean isManager = false;
		Boolean check = false;
		if (check) {
			try {
				if ( effectivePerson.isManager() ) {
					isManager = true;
				}
			} catch (Exception e) {
				check = false;
				Exception exception = new ExceptionFileInfoProcess(e, "判断用户是否是系统管理员时发生异常！user:" + effectivePerson.getDistinguishedName() );
				result.error(exception);
				logger.error(e, effectivePerson, request, null);
			}
		}
		
		try (EntityManagerContainer emc = EntityManagerContainerFactory.instance().create()) {
			Business business = new Business(emc);
			// 先判断需要操作的应用信息是否存在，根据ID进行一次查询，如果不存在不允许继续操作
			fileInfo = business.getFileInfoFactory().get(id);
			if (null == fileInfo) {
				throw new Exception("fileInfo{id:" + id + "} 文件信息不存在，无法继续删除.");
			}
			// 判断文档信息是否存在
			document = business.getDocumentFactory().get(fileInfo.getDocumentId());
			if (null == document) {
				throw new Exception("document{id:" + fileInfo.getDocumentId() + "} 文档信息不存在，无法继续删除.");
			}
			// 如果信息存在，再判断用户是否有操作的权限，如果没权限不允许继续操作
			if (!business.fileInfoEditAvailable(request, effectivePerson)) {
				throw new Exception( "fileInfo{name:" + effectivePerson.getDistinguishedName() + "} ，用户没有内容管理应用信息操作的权限！");
			}
			// 删除文件，并且删除记录及文档的关联信息
			StorageMapping mapping = ThisApplication.context().storageMappings().get(FileInfo.class, fileInfo.getStorage());

			// 从FTP上删除文件
			fileInfo.deleteContent(mapping);
			emc.beginTransaction(FileInfo.class);
			emc.beginTransaction(Document.class);
			emc.remove(fileInfo, CheckRemoveType.all);
			emc.commit();
			
			List<String> keys = new ArrayList<>();
			keys.add( "file.all" ); //清除文档的附件列表缓存
			keys.add( "file." + id  ); //清除指定ID的附件信息缓存
			keys.add( ApplicationCache.concreteCacheKey( "document", document.getId(), isAnonymous, isManager ) ); //清除文档的附件列表缓存
			ApplicationCache.notify( FileInfo.class, keys );
			
			keys.clear();
			keys.add(  ApplicationCache.concreteCacheKey( document.getId(), "view", isAnonymous, isManager ) ); //清除文档阅读缓存
			keys.add( ApplicationCache.concreteCacheKey( document.getId(), "get", isManager )  ); //清除文档信息获取缓存
			ApplicationCache.notify( Document.class, keys );
			
			// 成功删除一个附件信息
			logService.log(emc, effectivePerson.getDistinguishedName(), fileInfo.getName(), fileInfo.getAppId(),
					fileInfo.getId(), fileInfo.getDocumentId(), fileInfo.getId(), "FILE", "删除");

			Wo wo = new Wo();
			wo.setId( fileInfo.getId() );
			result.setData( wo );

		} catch (Exception e) {
			result.error(e);
			logger.error(e, effectivePerson, request, null);
		}
		return result;
	}
	public static class Wo extends WoId {

	}
}