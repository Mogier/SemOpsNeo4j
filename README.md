# SemOpsNeo4j
This programm runs the whole following process :
- Read a .csv file or a PostgreSQL database containing images and tags associated
- For each image, 
  1. Check if tags already exist in the specified Neo4j database
  2. Create non-existant tags and their semantic hierarchy
  3. Compute a semantic distance matrix between tags.

## Configuration
In order to run this programm, you'll need to do the following things :
- Get those programms : 
  1. [TreeGenerator](https://github.com/Mogier/terms-analysis)
  2. [GexfParser](https://github.com/Mogier/GexfParserForNeo4jDB)
- Open all three projects in Eclipse.
- Add TreeGenerator and GexfParser to SemOpsNeo4j's buildpath.
- Make sure you have a Wordnet dictionnary in your computer.
- Make sure you've edited the config.ini file in TreeGenerator repository.
- Edit the main class **SemOpsNeo4j.java** with your own parameters.
- Run :)
