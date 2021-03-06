/**
 * Copyright (C) 2011-2015 Incapture Technologies LLC
 *
 * This is an autogenerated license statement. When copyright notices appear below
 * this one that copyright supercedes this statement.
 *
 * Unless required by applicable law or agreed to in writing, software is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.
 *
 * Unless explicit permission obtained in writing this software cannot be distributed.
 */
package rapture.kernel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;

import rapture.common.CallingContext;
import rapture.common.EntitlementSet;
import rapture.common.exception.RaptureException;
import rapture.common.impl.jackson.MD5Utils;
import rapture.common.model.RaptureEntitlementGroup;
import rapture.common.model.RaptureUser;

public class EntitlementTest {

    private CallingContext ctx = ContextFactory.getKernelUser();
    private CallingContext localCtx = null;
    private UserApiImplWrapper userApi;
    private EntitlementApiImplWrapper entApi;
    private DocApiImplWrapper docApi;
    private String user = "heisenberg";

    @Before
    public void setup() {
        Kernel.initBootstrap();
        if (!Kernel.getAdmin().doesUserExist(ctx, user)) {
            Kernel.getAdmin().addUser(ctx, user, "A new user", MD5Utils.hash16("bluesky"), "heisenberg@mail.com");
        }
        if (!Kernel.getDoc().docRepoExists(ctx, "//privateAuthority")) {
            Kernel.getDoc().createDocRepo(ctx, "//privateAuthority", "NREP {} USING MEMORY {}");
        }
        localCtx = Kernel.getLogin().login(user, "bluesky", null);
        userApi = Kernel.getUser();
        entApi = Kernel.getEntitlement();
        docApi = Kernel.getDoc();
    }

    @Test
    public void testSuccessRead() {
        RaptureUser ruser = userApi.getWhoAmI(localCtx);
        assertEquals(user, ruser.getUsername());
    }

    @Test
    public void testModifyingEntitlements() {
        String group = "EntitlementTestGroup";
        entApi.addEntitlementGroup(ctx, group);
        entApi.addGroupToEntitlement(ctx, "user/read", group);

        assertTrue(entApi.getEntitlementGroup(ctx, group).getUsers().isEmpty());
        assertTrue(entApi.getEntitlement(ctx, "user/read").getGroups().contains(group));

        // should fail
        try {
            userApi.getWhoAmI(localCtx);
            fail("Entitlement still passed despite not being in group");
        } catch (RaptureException re) {
        }

        entApi.addUserToEntitlementGroup(ctx, group, user);

        // should work since user added to group
        try {
            userApi.getWhoAmI(localCtx);
        } catch (RaptureException re) {
            fail("Entitlement check failed: " + re.toString());
        }

        entApi.deleteEntitlementGroup(ctx, group);

        assertTrue(entApi.getEntitlementGroup(ctx, group) == null);

        // should still work
        try {
            userApi.getWhoAmI(localCtx);
        } catch (RaptureException re) {
            fail("Entitlement check failed: " + re.toString());
        }

        entApi.addEntitlementGroup(ctx,  "newgroup");
        entApi.addGroupToEntitlement(ctx, "user/read", "newgroup");

        // should fail
        try {
            userApi.getWhoAmI(localCtx);
            fail("Entitlement still passed despite not being in group");
        } catch (RaptureException re) {
        }
        
        entApi.deleteEntitlementGroup(ctx, "newgroup");

    }
    
    @Test
    public void testRemoveUserFromEntitlementGroup() {
        String groupName = "TestGroupTwo";
        entApi.addEntitlementGroup(ctx, groupName);
        entApi.addUserToEntitlementGroup(ctx, groupName, "UserThatStays");
        entApi.addUserToEntitlementGroup(ctx, groupName, "UserThatGoes");
        entApi.addUserToEntitlementGroup(ctx, groupName, "UserThatRemains");
        RaptureEntitlementGroup group = entApi.getEntitlementGroup(ctx, groupName);
        assertEquals(3, group.getUsers().size());
        entApi.removeUserFromEntitlementGroup(ctx, groupName, "UserThatGoes");
        group = entApi.getEntitlementGroup(ctx, groupName);
        assertEquals(2, group.getUsers().size());
    }
    
    @Test
    public void testDynamicEntitlements() {
        String group = "EntitlementTestGroup";
        String ent = EntitlementSet.Doc_putDoc.getPath().replaceAll("\\$.*", "privateAuthority/privatePath");
        entApi.addEntitlement(ctx, ent, group);
        entApi.addEntitlementGroup(ctx, group);
        entApi.addGroupToEntitlement(ctx, ent, group);

        assertTrue(entApi.getEntitlementGroup(ctx, group).getUsers().isEmpty());
        assertTrue(entApi.getEntitlement(ctx, ent).getGroups().contains(group));
        
        String content = "{\"key\" : \"value\" }";
        
        try {
            docApi.putDoc(localCtx, "//privateAuthority/privatePath/x1", content);
            fail("Somehow passed dynamic entitlement check--should've rejected");
        }
        catch (RaptureException re) {
        }
        
        entApi.addUserToEntitlementGroup(ctx, group, user);

        try {
            docApi.putDoc(localCtx, "//privateAuthority/privatePath/x1", content);
        }
        catch (RaptureException re) {
            fail("Entitlement check rejected when shouldve passed: " + re.toString());
        }
        
        
    }

}
