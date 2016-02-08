/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.displayer.script;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import org.xwiki.component.annotation.Component;
import org.xwiki.script.service.ScriptService;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.security.authorization.AccessDeniedException;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;

/**
 * Make the Application API available to scripting.
 * 
 * @version $Id$
 */
@Component
@Named("doc")
@Singleton
public class DisplayerScriptService implements ScriptService
{
    @Inject
    protected Provider<XWikiContext> xcontextProvider;
    @Inject
    ContextualAuthorizationManager authorization;
    @Inject
    @Named("local")
    protected EntityReferenceSerializer<String> serializer;

    public String display(String propName) throws XWikiException, UnsupportedEncodingException, AccessDeniedException
    {
        XWikiContext context = this.xcontextProvider.get();
        
        XWikiDocument doc = context.getDoc();
        DocumentReference docRef = doc.getDocumentReference();
        this.authorization.checkAccess(Right.VIEW, docRef);

        BaseObject obj = doc.getFirstObject(propName, context);
        if(obj == null)
            return "";
        DocumentReference classRef = obj.getXClass(context).getReference();
        String docId = encodeId(serializer.serialize(docRef));
        String classId = encodeId(serializer.serialize(classRef));
        String propId = encodeId(propName);
        String objNumber = "0";
        String id = docId+"_"+classId+"_"+objNumber+"_"+propId;
        String oldValue = doc.display(propName, context);
        if(oldValue.equals("")) {
            oldValue = "-";
        }

        // Check if the user has edit rights in the current document. If not, return the normal XWiki view displayer.
        if(this.authorization.hasAccess(Right.EDIT, docRef)) {
            String valueView = "(% data-xwiki-property=\"" + id + "\"%)(((" + oldValue + ")))";
            String valueEdit = "(% data-xwiki-property-edit=\"" + id + "\" style=\"display:none;\" %)(((" + doc.display(propName, "edit", context) + ")))";
            return valueView + valueEdit + "{{html clean=\"false\"}}<br>{{/html}}";
        }
        else {
            return oldValue;
        }
    }

    private String encodeId(String data) throws UnsupportedEncodingException {
        return URLEncoder.encode(data.replace("_", "%5F").replace("+", "%20"), "UTF-8");
    }
}
