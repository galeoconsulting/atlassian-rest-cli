/*
 * Copyright 2011 Leonid Maslov<leonidms@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

[
        execute: {

            //--configure issue types--//
            final String imageUrl = "/images/icons/genericissue.gif"
            def issueType1 = migrationHelper.createIssueType(name: "Bug 1", sequence: 10, description: "Bug type 1",
                    iconurl: imageUrl)
            def issueType2 = migrationHelper.createIssueType(name: "Task 1", sequence: 11, description: "Task type 1",
                    iconurl: imageUrl)
            def issueTypes = [issueType1, issueType2]

            //--configure projects--//
            def project1 = migrationHelper.createProject(name: "Project 1", key: "AAA", description: "Auto Abra Kadabra",
                    lead: "admin");
            def project2 = migrationHelper.createProject(name: "Project 2", key: "TST", description: "Test Project",
                    lead: "admin");
            def projects = [project1, project2]


            println(">> Issue types created: ${issueTypes.collect {it.name}}")
            println(">> Projects created: ${projects.collect {it.name}}")
        },
        undo: { throw new IllegalArgumentException("Not implemented") }
]
