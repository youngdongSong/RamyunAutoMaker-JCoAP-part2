/* Copyright [2011] [University of Rostock]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *****************************************************************************/

package org.ws4d.coap.server;

import org.ws4d.coap.Constants;
import org.ws4d.coap.connection.BasicCoapChannelManager;
import org.ws4d.coap.rest.CoapResource;
import org.ws4d.coap.rest.CoapResourceServer;

/**
 * @author Christian Lerche <christian.lerche@uni-rostock.de>
 */
public class BasicCoapServer {
    private int port = Constants.COAP_DEFAULT_PORT;
    private CoapResourceServer resourceServer = null;
    static int counter = 0;
    
    public BasicCoapServer(int port){
    	
    }
    
    public BasicCoapServer(){
    	
    }
    
    public void init() {
	    BasicCoapChannelManager.getInstance().setMessageId(2000);
		if (resourceServer != null)
			resourceServer.stop();
		resourceServer = new CoapResourceServer();
    }
    
    public boolean addResource( CoapResource resource ){
    	if( resourceServer != null ) {
    		resourceServer.createResource( resource );
    		return true;
    	}
    	
    	return false;
    }
    
    public void run() {
		try {
			System.out.println("=== Starting Server ===");
		    resourceServer.start();
		} catch (Exception e) {
		    e.printStackTrace();
		}
    }
}
