#ifndef BINARY_HPP
#define BINARY_HPP
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

#include <proton/export.hpp>
#include <proton/types_fwd.hpp>

#include <iosfwd>
#include <vector>

namespace proton {

/// Arbitrary binary data.
class binary : public std::vector<char> {
  public:
    typedef std::vector<char> byte_vector;

    explicit binary() : byte_vector() {}
    explicit binary(size_t n) : byte_vector(n) {}
    explicit binary(size_t n, char x) : byte_vector(n, x) {}
    template <class Iter> binary(Iter first, Iter last) : byte_vector(first, last) {}
    explicit binary(const std::string& s) : byte_vector(s.begin(), s.end()) {}

    std::string str() const { return std::string(begin(), end()); }

    binary& operator=(const binary& x) { byte_vector::operator=(x); return *this; }
    binary& operator=(const std::string& x) { assign(x.begin(), x.end()); return *this; }
};

PN_CPP_EXTERN std::ostream& operator<<(std::ostream&, const binary&);

}

#endif // BINARY_HPP
