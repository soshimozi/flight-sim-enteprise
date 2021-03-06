function initAutoCompleteIP(myname, myvalue)
{
    $(myname).autocomplete(
        {
            source: function (request, response)
            {
                $.ajax(
                    {
                        url: "/admin/lookupip.jsp",
                        data: {
                            startsWith: request.term,
                            accountType: 2 //2 = person
                        },
                        type: "POST",  // POST transmits in querystring format (key=value&key1=value1) in utf-8
                        dataType: "json",   //return data in json format
                        success: function (data)
                        {
                            var results = [];

                            $.map(data.ips, function (item)
                            {
                                var itemToAdd =  { label: item.label, id: item.value };
                                results.push(itemToAdd);
                            });

                            return response(results);
                        }
                    });
            },
            minLength: 1,
            delay: 450,
            selectFirst: false,
            autoFocus: true,
            select: function(event, ui)
            {
                $(myname).val(ui.item.label);
                $(myvalue).val(ui.item.id);
            }
        });
}
