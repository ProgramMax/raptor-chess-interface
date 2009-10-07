/**
 * New BSD License
 * http://www.opensource.org/licenses/bsd-license.php
 * Copyright (c) 2009, RaptorProject (http://code.google.com/p/raptor-chess-interface/)
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
 * Neither the name of the RaptorProject nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package raptor.connector.ics.chat;

import raptor.chat.ChatEvent;
import raptor.chat.ChatType;
import raptor.connector.ics.IcsUtils;
import raptor.util.RaptorStringTokenizer;

public class PartnerTellEventParser extends ChatEventParser {
	private static final String PTELL_IDENTIFIER = "(your partner) tells you: ";

	public PartnerTellEventParser() {
	}

	/**
	 * Returns null if text does not match the event this class produces.
	 */
	@Override
	public ChatEvent parse(String text) {
		if (text.length() < 1500) {
			int i = text.indexOf(PTELL_IDENTIFIER);
			if (i != -1) {
				RaptorStringTokenizer stringtokenizer = new RaptorStringTokenizer(
						text, " ");
				String s1 = stringtokenizer.nextToken();
				if (stringtokenizer.nextToken().equals("(your"))
					return new ChatEvent(IcsUtils.removeTitles(s1),
							ChatType.PARTNER_TELL, text);
			}
			return null;
		}
		return null;
	}
}