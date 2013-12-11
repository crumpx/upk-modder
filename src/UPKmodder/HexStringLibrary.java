/*
 * Copyright (C) 2013 Rachel Norman
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package UPKmodder;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 *
 * @author Amineri
 */
public class HexStringLibrary 
{
    public String convertIntToHexString(int I)
    {
        String sOutString = "";
        ByteBuffer b = ByteBuffer.allocate(4);
        b.order(ByteOrder.LITTLE_ENDIAN); 
        b.putInt(I);

        byte[] result = b.array();
        for(int J = 0; J < 4 ; J++)
        {
            int temp = result[J] & 0xFF;
            sOutString = sOutString + " " + String.format("%2s", Integer.toHexString(temp)).replace(' ', '0').toUpperCase();
        }
        return sOutString;
    }
    
}