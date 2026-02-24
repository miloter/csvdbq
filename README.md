# CsvDbQ
### CsvDbQ es un sistema de consulta de información en ficheros CSV.
### CsvDbQ usa un lenguaje parecido al SQL, al que se ha llamado QlCsv para realizar consultas en dichos ficheros.
### Las características de QlCsv son las siguientes:

* Utiliza un lenguaje declarativo similar al SQL pero no es SQL.
* No distingue entre mayúsculas y minúsculas.
* Solo tiene 4 tipos de datos: number, string, boolean y null.
* El tipo boolean solo admite los valores en minúsculas true o false.
* El tipo null solo tiene un valor que es el mismo null en minúsculas.
* QlCsv admite archivos CSV que usen como entrecomillado la comilla doble (“), o la simple (‘) y como separador de campos uno de: punto y coma (;), coma (,) o tabulador (   ).
* Los ficheros CSV se guardan con la marca de BOM UTF-8 para compatibilidad con Excel.

Puede probar la aplicación en Windows descargando [CsvDbQ para Windows](CsvDbQ-0.1.2_win_x64.zip) que incluye un jre-21 integrado y el ejecutable de la aplicación (generado con launch4j y Java jdeps/jlink)

Para lanzar la aplicación desde los *.class se necesita tener instalado Java 21 o superior, y se puede lanzar desde el directorio raiz del proyecto ejecutando:
```
javaw -cp "./bin;./lib/text-scanner-1.0.3.jar" es.facite.csvdbq.gui.view.App
```

Se puede obtener un manual en línea (siempre en construcción) en [https://assets.facite.es/csvdbq/](https://assets.facite.es/csvdbq/)

### Comencemos con una introducción al entorno:

![Imagen de CsvDbQ](csvdbq.jpg)

<br>

> Establezcamos el directorio de trabajo, donde se ejecutan los comandos o consultas QlCsv:
```
> set dir = .
Nuevo directorio de trabajo: D:\google-drive\desarrollo\java\projects\facite-api\csvdbq
Comando ejecutado en 1 ms.
```

<br>

> Podemos usar el punto (.) para establecer como directorio de trabajo el mismo donde se ejecuta el entorno o cualquier otro:
```
> set dir = c:/temp
Nuevo directorio de trabajo: c:\temp
Comando ejecutado en 0 ms.
```

<br>

> Mostrar todos los registros de una tabla llamada paises (archivo países.csv) :
```
> select * from paises
+---------------------------------+-----------------------------------------+------------+
| pais                            | capital                                 | poblacion  |
+---------------------------------+-----------------------------------------+------------+
| Afganistán                      | Kabul                                   | 31936000   |
| Albania                         | Tirana                                  | 2875000    |
| Alemania                        | Berlín                                  | 83082000   |
..
| Palestina                       | Ramala                                  | 4918000    |
+---------------------------------+-----------------------------------------+------------+
198 filas devueltas.
Comando ejecutado en 10 ms.
```

<br>

> Supongamos que tenemos una CSV con filas repetidas llamada ‘paises repetidos.csv’:
```
> select * from 'paises repetidos'
+-------------+---------+-----------------+
| pais        | capital | poblacion total |
+-------------+---------+-----------------+
| Alemania    | Berlín  | 83082000        |
| España      | Madrid  | 46791000        |
| Reino Unido | Londres | 66636000        |
| España      | Madrid  | 46791000        |
| España      | Madrid  | 46791000        |
| Alemania    | Berlín  | 83082000        |
| España      | Madrid  | 46791000        |
| Reino Unido | Londres | 66636000        |
+-------------+---------+-----------------+
8 filas devueltas.
Comando ejecutado en 2 ms.
```

<br>

> Si ahora queremos mostrar solo las filas diferentes y únicamente las columnas país y ‘poblacion total’:
```
> select distinct pais, [poblacion total] from 'paises repetidos'
+-------------+-------------------+
| pais        | [poblacion total] |
+-------------+-------------------+
| Alemania    | 83082000          |
| España      | 46791000          |
| Reino Unido | 66636000          |
+-------------+-------------------+
3 filas devueltas.
Comando ejecutado en 5 ms.
```
Cuando una tabla no tiene el formado de identificador, la nombraremos entre comillas simples como a **‘paises repetidos’** y cuando sea una columna la que no tiene formato de identificador, usaremos los corchetes como en **[población total]**.

<br>

> Podemos dar un alias a un nombre de columna con la cláusula AS:
```
> select distinct pais as 'Nombre del País', [poblacion total] as 'Población' from 'paises repetidos'
+-----------------+-----------+
| Nombre del País | Población |
+-----------------+-----------+
| Alemania        | 83082000  |
| España          | 46791000  |
| Reino Unido     | 66636000  |
+-----------------+-----------+
3 filas devueltas.
Comando ejecutado en 2 ms.
```

<br>

> Podemos filtrar los registros usando la cláusula WHERE:
```
>  select pais, poblacion from paises where poblacion between 1000000 and 2000000
+-------------------+-----------+
| pais              | poblacion |
+-------------------+-----------+
| Baréin            | 1608000   |
| Estonia           | 1323000   |
| Guinea Ecuatorial | 1382000   |
| Guinea-Bissau     | 1599000   |
| Letonia           | 1920000   |
| Mauricio          | 1268000   |
| Suazilandia       | 1106000   |
| Timor Oriental    | 1275000   |
| Trinidad y Tobago | 1361000   |
| Yibuti            | 1064000   |
+-------------------+-----------+
10 filas devueltas.
Comando ejecutado en 4 ms.
```

<br>

> Para agrupar registros usaremos la claúsula GROUP BY, indicando las funciones de agrupación:
```
> select clase1, clase2, count(*) as total from groups group by clase1, clase2
+--------+--------+-------+
| clase1 | clase2 | total |
+--------+--------+-------+
| a      | 1      | 3     |
| c      | 2      | 6     |
| b      | 1      | 2     |
| c      | 1      | 2     |
| a      | 2      | 1     |
| b      | 2      | 2     |
| d      | 11     | 3     |
+--------+--------+-------+
7 filas devueltas.
Comando ejecutado en 2 ms.
```

<br>

> Si se quiere filtrar el resultado de las funciones de agrupado usaremos la cláusula HAVING:
```
> select clase1, clase2, count(*) as total from groups group by clase1, clase2 having count(*) >= 3
+--------+--------+-------+
| clase1 | clase2 | total |
+--------+--------+-------+
| a      | 1      | 3     |
| c      | 2      | 6     |
| d      | 11     | 3     |
+--------+--------+-------+
3 filas devueltas.
Comando ejecutado en 3 ms.
```

<br>

> Los resultados se pueden ordenar tanto de forma ascendente con ASC (valor por defecto), como descendente con DESC:
```
> select clase1, clase2, count(*) as total
      from groups
      group by clase1, clase2
      order by clase1, clase2 desc
+--------+--------+-------+
| clase1 | clase2 | total |
+--------+--------+-------+
| a      | 2      | 1     |
| a      | 1      | 3     |
| b      | 2      | 2     |
| b      | 1      | 2     |
| c      | 2      | 6     |
| c      | 1      | 2     |
| d      | 11     | 3     |
+--------+--------+-------+
7 filas devueltas.
Comando ejecutado en 1 ms.
```

<br>

> Podemos limitar el resultado devuelto a un número de registros:
```
> select * from paises order by poblacion desc limit 5
+----------------+------------------+------------+
| pais           | capital          | poblacion  |
+----------------+------------------+------------+
| China          | Pekín            | 1395261000 |
| India          | Nueva Delhi      | 1375898000 |
| Estados Unidos | Washington D. C. | 329071000  |
| Indonesia      | Yakarta          | 266614000  |
| Pakistán       | Islamabad        | 216823000  |
+----------------+------------------+------------+
5 filas devueltas.
Comando ejecutado en 3 ms.
```

<br>

> Además podemos decir que el número máximo de registros devueltos sea desde una posición Determinada, comenzando en 0:
```
> select * from paises order by poblacion desc limit 5, 5
+-----------+----------+-----------+
| pais      | capital  | poblacion |
+-----------+----------+-----------+
| Brasil    | Brasilia | 210461000 |
| Nigeria   | Abuya    | 209058000 |
| Bangladés | Daca     | 176198000 |
| Rusia     | Moscú    | 147043000 |
| Japón     | Tokio    | 126398000 |
+-----------+----------+-----------+
5 filas devueltas.
Comando ejecutado en 3 ms.
```
Esta última instrucción ha devuelto 5 registros comenzando desde el sexto (posición 5).

<br>
 
> Podemos juntar tablas mediante la cláusula JOIN, en el ejemplo a continuación tenemos una tabla de empleados y otra con las direcciones, queremos juntarlas a través del común id:
```
> select * from empleados
+----+---------+-----------+-----------+--------------+
| id | nombre  | apellido1 | apellido2 | fecha_alta   |
+----+---------+-----------+-----------+--------------+
| 1  | Miguel  | Smith     | Russell   | [1997-12-29] |
| 2  | John    | Curtis    | Travell   | [1998-07-19] |
| 3  | Sarah   | Park      | Michaels  | [1994-02-25] |
| 4  | Ruth    | Perkins   | O'Hara    | [1999-01-23] |
| 5  | Stephen | Reynolds  | King      | [2000-08-17] |
+----+---------+-----------+-----------+--------------+
5 filas devueltas.
Comando ejecutado en 8 ms.

> select * from dir_empleados
+----+------------------+-----+-----------+---------+-------------+------------+
| id | via              | num | mas       | cp      | localidad   | provincia  |
+----+------------------+-----+-----------+---------+-------------+------------+
| 1  | Avda Francia     | 57  | 4ª Planta | [41006] | Sevilla     | Sevilla    |
| 2  | Calle San Emilio | 99  | 3ª Planta | [08205] | Igualada    | Barcelona  |
| 3  | Calle Barcos     | 17  | 1º        | [36504] | Villa Santa | Pontevedra |
| 4  | Avda Estaciones  | 23  | Bajo      | [28160] | Alcobendas  | Madrid     |
| 5  | Plaza España     | 1   | Bajo      | [28690] | Las Rozas   | Madrid     |
+----+------------------+-----+-----------+---------+-------------+------------+
5 filas devueltas.
Comando ejecutado en 8 ms.

> select id, nombre, apellido1, apellido2, via, num, mas, cp, localidad
      from join(empleados; dir_empleados; id)
+----+---------+-----------+-----------+------------------+-----+-----------+---------+-------------+
| id | nombre  | apellido1 | apellido2 | via              | num | mas       | cp      | localidad   |
+----+---------+-----------+-----------+------------------+-----+-----------+---------+-------------+
| 1  | Miguel  | Smith     | Russell   | Avda Francia     | 57  | 4ª Planta | [41006] | Sevilla     |
| 2  | John    | Curtis    | Travell   | Calle San Emilio | 99  | 3ª Planta | [08205] | Igualada    |
| 3  | Sarah   | Park      | Michaels  | Calle Barcos     | 17  | 1º        | [36504] | Villa Santa |
| 4  | Ruth    | Perkins   | O'Hara    | Avda Estaciones  | 23  | Bajo      | [28160] | Alcobendas  |
| 5  | Stephen | Reynolds  | King      | Plaza España     | 1   | Bajo      | [28690] | Las Rozas   |
+----+---------+-----------+-----------+------------------+-----+-----------+---------+-------------+

5 filas devueltas.
Comando ejecutado en 8 ms.
```
Se puede observar que en la cláusula **JOIN** los argumentos se separan con punto y coma (**;**) y no con coma (**,**) debido a que además de tablas se puede usar como argumento, una cláusula **SELECT** que puede contener comas, o una cláusula **UNION**.

<br>

> También se pueden unir tablas con UNION, ambas tablas deben tener el mismo número de campos:
```
> select * from paises1
+--------------+---------------------+-----------+
| pais         | capital             | poblacion |
+--------------+---------------------+-----------+
| Brunei       | Bandar Seri Begawan | 429000    |
| Bulgaria     | Sofía               | 7004000   |
| Burkina Faso | Uagadugú            | 20571000  |
| Burundi      | Buyumbura           | 10836000  |
| Bután        | Timbu               | 740000    |
+--------------+---------------------+-----------+
5 filas devueltas.
Comando ejecutado en 1 ms.

> select * from paises2
+-----------------+-------------------+-----------+
| pais            | capital           | poblacion |
+-----------------+-------------------+-----------+
| Colombia        | Bogotá            | 45878000  |
| Comoras         | Moroni            | 862000    |
| Corea Del Norte | Pyongyang         | 25694000  |
| Corea Del Sur   | Seúl              | 51843000  |
| Costa De Marfil | Yamusukro, Abiyán | 25825000  |
| Costa Rica      | San José          | 5032000   |
| Croacia         | Zagreb            | 4080000   |
| Cuba            | La Habana         | 11212000  |
+-----------------+-------------------+-----------+
8 filas devueltas.
Comando ejecutado en 1 ms.

> select * from union(paises1; paises2)
+-----------------+---------------------+-----------+
| pais            | capital             | poblacion |
+-----------------+---------------------+-----------+
| Brunei          | Bandar Seri Begawan | 429000    |
| Bulgaria        | Sofía               | 7004000   |
| Burkina Faso    | Uagadugú            | 20571000  |
| Burundi         | Buyumbura           | 10836000  |
| Bután           | Timbu               | 740000    |
| Colombia        | Bogotá              | 45878000  |
| Comoras         | Moroni              | 862000    |
| Corea Del Norte | Pyongyang           | 25694000  |
| Corea Del Sur   | Seúl                | 51843000  |
| Costa De Marfil | Yamusukro, Abiyán   | 25825000  |
| Costa Rica      | San José            | 5032000   |
| Croacia         | Zagreb              | 4080000   |
| Cuba            | La Habana           | 11212000  |
+-----------------+---------------------+-----------+
13 filas devueltas.
Comando ejecutado en 2 ms.
```
Se puede observar que en la cláusula **UNION** los argumentos se separan con punto y coma (**;**) y no con coma (**,**) debido a que además de tablas se puede usar como argumento, una cláusula **SELECT** que puede contener comas, o una cláusula **JOIN**.
